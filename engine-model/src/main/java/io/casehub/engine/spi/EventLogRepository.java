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
package io.casehub.engine.spi;

import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.smallrye.mutiny.Uni;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Storage provider for immutable {@link EventLog} entries. All writes are append-only.
 * Implementations handle their own session/transaction management.
 */
public interface EventLogRepository {

  /** Append an event. Sets {@code eventLog.id} and {@code eventLog.seq} on completion. */
  Uni<Void> append(EventLog eventLog);

  /**
   * Append an event and return its generated id. Sets {@code eventLog.id} and {@code eventLog.seq}.
   */
  Uni<Long> appendAndReturnId(EventLog eventLog);

  /** Find an event by its generated id. Returns {@code null} if not found. */
  Uni<EventLog> findById(Long id);

  /**
   * Find all scheduling-lifecycle events (WORKER_SCHEDULED, WORKER_EXECUTION_STARTED,
   * WORKER_EXECUTION_COMPLETED) for the given case and worker, ordered by seq ascending.
   */
  Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId);

  /**
   * Find all events matching the given types across all cases, ordered by seq ascending. Used by
   * recovery to replay in-flight workers.
   */
  Uni<List<EventLog>> findByTypes(Collection<CaseHubEventType> types);

  /** Find events for a specific case matching the given types, ordered by seq ascending. */
  Uni<List<EventLog>> findByCaseAndTypes(UUID caseId, Collection<CaseHubEventType> types);

  /** Find events for a specific case, worker, and event type (all criteria must match). */
  Uni<List<EventLog>> findByCaseAndWorkerAndType(
      UUID caseId, String workerId, CaseHubEventType type);
}
