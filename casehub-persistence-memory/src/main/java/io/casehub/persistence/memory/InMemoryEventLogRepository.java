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
package io.casehub.persistence.memory;

import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.spi.EventLogRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Alternative
@ApplicationScoped
public class InMemoryEventLogRepository implements EventLogRepository {

  private final AtomicLong idSeq = new AtomicLong(0);
  private final AtomicLong seqCounter = new AtomicLong(0);
  private final ConcurrentHashMap<Long, EventLog> store = new ConcurrentHashMap<>();

  /**
   * Read-write lock to ensure happens-before relationship between writes and reads.
   * ConcurrentHashMap iterators are weakly consistent — a write via put() may not be visible to an
   * immediately following values().stream() without a memory barrier. Multiple concurrent reads are
   * allowed; writes require exclusive access. Read lock permits parallel find* queries; write lock
   * serializes append operations.
   */
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  @Override
  public Uni<Void> append(EventLog eventLog) {
    rwLock.writeLock().lock();
    try {
      eventLog.id = idSeq.incrementAndGet();
      eventLog.setSeq(seqCounter.incrementAndGet());
      store.put(eventLog.id, eventLog);
      return Uni.createFrom().voidItem();
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  @Override
  public Uni<Long> appendAndReturnId(EventLog eventLog) {
    rwLock.writeLock().lock();
    try {
      eventLog.id = idSeq.incrementAndGet();
      eventLog.setSeq(seqCounter.incrementAndGet());
      store.put(eventLog.id, eventLog);
      return Uni.createFrom().item(eventLog.id);
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  @Override
  public Uni<EventLog> findById(Long id) {
    rwLock.readLock().lock();
    try {
      return Uni.createFrom().item(store.get(id));
    } finally {
      rwLock.readLock().unlock();
    }
  }

  @Override
  public Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId) {
    rwLock.readLock().lock();
    try {
      List<EventLog> result =
          store.values().stream()
              .filter(e -> caseId.equals(e.getCaseId()) && workerId.equals(e.getWorkerId()))
              .filter(
                  e ->
                      e.getEventType() == CaseHubEventType.WORKER_SCHEDULED
                          || e.getEventType() == CaseHubEventType.WORKER_EXECUTION_STARTED
                          || e.getEventType() == CaseHubEventType.WORKER_EXECUTION_COMPLETED)
              .toList();
      return Uni.createFrom().item(result);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  @Override
  public Uni<List<EventLog>> findByTypes(Collection<CaseHubEventType> types) {
    rwLock.readLock().lock();
    try {
      List<EventLog> result =
          store.values().stream()
              .filter(e -> types.contains(e.getEventType()))
              .sorted(Comparator.comparingLong(EventLog::getSeq))
              .toList();
      return Uni.createFrom().item(result);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  @Override
  public Uni<List<EventLog>> findByCaseAndTypes(UUID caseId, Collection<CaseHubEventType> types) {
    rwLock.readLock().lock();
    try {
      List<EventLog> result =
          store.values().stream()
              .filter(e -> caseId.equals(e.getCaseId()) && types.contains(e.getEventType()))
              .sorted(Comparator.comparingLong(EventLog::getSeq))
              .toList();
      return Uni.createFrom().item(result);
    } finally {
      rwLock.readLock().unlock();
    }
  }

  @Override
  public Uni<List<EventLog>> findByCaseAndWorkerAndType(
      UUID caseId, String workerId, CaseHubEventType type) {
    rwLock.readLock().lock();
    try {
      List<EventLog> result =
          store.values().stream()
              .filter(
                  e ->
                      caseId.equals(e.getCaseId())
                          && workerId.equals(e.getWorkerId())
                          && type == e.getEventType())
              .toList();
      return Uni.createFrom().item(result);
    } finally {
      rwLock.readLock().unlock();
    }
  }
}
