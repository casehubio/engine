/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.engine;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks case-level quiescence — the point where no workers are executing and no context change
 * events are in-flight. Unlike {@link SignalSettlementTracker} which tracks a single signal's wave
 * of workers, this tracks ALL cascading waves until the case reaches a true idle state.
 *
 * <p>Two counters per case:
 *
 * <ul>
 *   <li>{@code activeWorkers} — dispatched but not yet completed workers
 *   <li>{@code pendingContextChanges} — CONTEXT_CHANGED events published but not yet consumed by
 *       {@code CaseContextChangedEventHandler}
 * </ul>
 *
 * <p>Quiescence is reached when at least one evaluation has drained, both counters are at or below
 * zero, and a future has been registered via {@link #register(UUID)}.
 *
 * <p>State is created eagerly by increment methods ({@code onWorkerDispatched}, {@code
 * onContextChangePublished}) so counter updates are never lost — even when {@code
 * awaitQuiescence()} is called after the signal has already been processed.
 *
 * <p>Refs casehubio/engine#610.
 */
@ApplicationScoped
public class QuiescenceTracker {

  private final ConcurrentHashMap<UUID, QuiescenceState> trackers = new ConcurrentHashMap<>();

  /**
   * Registers interest in quiescence for the given case. Creates a future that completes when the
   * case reaches quiescence. If activity has already been tracked and completed, resolves
   * immediately.
   */
  public CompletableFuture<Void> register(UUID caseId) {
    QuiescenceState state = trackers.computeIfAbsent(caseId, k -> new QuiescenceState());
    state.lock.lock();
    try {
      if (state.future == null) {
        state.future = new CompletableFuture<>();
      }
      return state.future;
    } finally {
      state.lock.unlock();
    }
  }

  public boolean isTracking(UUID caseId) {
    return trackers.containsKey(caseId);
  }

  public void onWorkerDispatched(UUID caseId) {
    QuiescenceState state = trackers.computeIfAbsent(caseId, k -> new QuiescenceState());
    state.lock.lock();
    try {
      state.activeWorkers.incrementAndGet();
    } finally {
      state.lock.unlock();
    }
  }

  public void onWorkerCompleted(UUID caseId) {
    QuiescenceState state = trackers.get(caseId);
    if (state != null) {
      state.lock.lock();
      try {
        state.activeWorkers.decrementAndGet();
      } finally {
        state.lock.unlock();
      }
      tryResolve(caseId);
    }
  }

  public void onContextChangePublished(UUID caseId) {
    QuiescenceState state = trackers.computeIfAbsent(caseId, k -> new QuiescenceState());
    state.lock.lock();
    try {
      state.pendingContextChanges.incrementAndGet();
    } finally {
      state.lock.unlock();
    }
  }

  // Clamped to never go below zero — seed CCs (from signals/case creation) are consumed
  // without a matching onContextChangePublished, so unclamped decrement would drive the
  // counter negative and cause premature resolution.
  public void onContextChangeConsumed(UUID caseId) {
    QuiescenceState state = trackers.get(caseId);
    if (state != null) {
      state.lock.lock();
      try {
        if (state.pendingContextChanges.get() > 0) {
          state.pendingContextChanges.decrementAndGet();
        }
      } finally {
        state.lock.unlock();
      }
    }
  }

  /**
   * Called by {@link CaseEvaluationSerializer} when drainPending finds no more pending work. This
   * signals that at least one evaluation cycle has completed for the case.
   */
  public void onEvaluationDrained(UUID caseId) {
    QuiescenceState state = trackers.get(caseId);
    if (state != null) {
      state.lock.lock();
      try {
        state.drainCount.incrementAndGet();
      } finally {
        state.lock.unlock();
      }
      tryResolve(caseId);
    }
  }

  public void tryResolve(UUID caseId) {
    QuiescenceState state = trackers.get(caseId);
    if (state != null) {
      state.lock.lock();
      try {
        if (state.future != null
            && state.drainCount.get() > 0
            && state.activeWorkers.get() <= 0
            && state.pendingContextChanges.get() <= 0) {
          state.future.complete(null);
          trackers.remove(caseId);
        }
      } finally {
        state.lock.unlock();
      }
    }
  }

  public void remove(UUID caseId) {
    QuiescenceState state = trackers.remove(caseId);
    if (state != null && state.future != null && !state.future.isDone()) {
      state.future.cancel(false);
    }
  }

  private static class QuiescenceState {
    final ReentrantLock lock = new ReentrantLock();
    final AtomicInteger activeWorkers = new AtomicInteger(0);
    final AtomicInteger pendingContextChanges = new AtomicInteger(0);
    final AtomicInteger drainCount = new AtomicInteger(0);
    volatile CompletableFuture<Void> future;
  }
}
