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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jboss.logging.Logger;

public class CaseEvaluationSerializer implements io.casehub.engine.common.spi.Resettable {

  private static final Logger LOG = Logger.getLogger(CaseEvaluationSerializer.class);

  private final QuiescenceTracker quiescenceTracker;

  private final ConcurrentHashMap<UUID, CaseGate> gates = new ConcurrentHashMap<>();
  private final ReentrantLock lifecycleLock = new ReentrantLock(true);
  private final Condition evaluationsDrained = lifecycleLock.newCondition();
  private int activeEvaluations;
  private boolean resetting;

  public CaseEvaluationSerializer(QuiescenceTracker quiescenceTracker) {
    this.quiescenceTracker = quiescenceTracker;
  }

  public void submit(UUID caseId, Runnable evaluator) {
    CaseGate gate;
    boolean runNow = false;
    lifecycleLock.lock();
    try {
      while (resetting) {
        evaluationsDrained.awaitUninterruptibly();
      }
      gate = gates.computeIfAbsent(caseId, CaseGate::new);
      if (gate.evaluating) {
        gate.pendingEvaluators.addLast(evaluator);
        return;
      }
      gate.evaluating = true;
      activeEvaluations++;
      runNow = true;
    } finally {
      lifecycleLock.unlock();
    }

    if (runNow) {
      try {
        runEvaluator(caseId, evaluator, false);
      } finally {
        drainPending(caseId, gate);
      }
    }
  }

  public void evict(UUID caseId) {
    lifecycleLock.lock();
    try {
      CaseGate gate = gates.get(caseId);
      if (gate == null) {
        return;
      }
      if (gate.evaluating) {
        gate.evictWhenDrained = true;
      } else {
        gates.remove(caseId, gate);
      }
    } finally {
      lifecycleLock.unlock();
    }
  }

  @Override
  public void reset() {
    lifecycleLock.lock();
    try {
      resetting = true;
      while (activeEvaluations > 0) {
        evaluationsDrained.awaitUninterruptibly();
      }
      gates.clear();
      resetting = false;
      evaluationsDrained.signalAll();
    } finally {
      lifecycleLock.unlock();
    }
  }

  private void drainPending(UUID caseId, CaseGate gate) {
    while (true) {
      Runnable next;
      lifecycleLock.lock();
      try {
        next = gate.pendingEvaluators.pollFirst();
        if (next == null) {
          gate.evaluating = false;
          if (gate.evictWhenDrained) {
            gates.remove(caseId, gate);
          }
          if (quiescenceTracker != null) {
            try {
              quiescenceTracker.onEvaluationDrained(caseId);
            } catch (Exception e) {
              LOG.warnf(e, "Failed to report drained evaluation for caseId=%s", caseId);
            }
          }
          activeEvaluations--;
          if (activeEvaluations == 0) {
            evaluationsDrained.signalAll();
          }
          return;
        }
      } finally {
        lifecycleLock.unlock();
      }
      runEvaluator(caseId, next, true);
    }
  }

  private void runEvaluator(UUID caseId, Runnable evaluator, boolean queued) {
    try {
      evaluator.run();
    } catch (Exception e) {
      LOG.errorf(e, "%s evaluation failed for caseId=%s", queued ? "Queued" : "Initial", caseId);
    }
  }

  private static final class CaseGate {
    final UUID caseId;
    final Deque<Runnable> pendingEvaluators = new ArrayDeque<>();
    boolean evaluating;
    boolean evictWhenDrained;

    CaseGate(UUID caseId) {
      this.caseId = caseId;
    }
  }
}
