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

public interface EventLogRepository {

  /** Append an event to the log. Populates eventLog.id and eventLog.seq after append. */
  Uni<Void> append(EventLog eventLog);

  /**
   * Append an event and flush, returning the generated id. Used by WorkerScheduleEventHandler to
   * get the id before scheduling a Quartz job.
   */
  Uni<Long> appendAndReturnId(EventLog eventLog);

  /** Load an event by id. Returns null if not found. */
  Uni<EventLog> findById(Long id);

  /**
   * Find WORKER_SCHEDULED, WORKER_EXECUTION_STARTED, and WORKER_EXECUTION_COMPLETED events for a
   * specific case+worker. Used by WorkerScheduleEventHandler for idempotency checking.
   */
  Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId);

  /**
   * Find all events matching any of the given types, ordered by seq ascending. Used by
   * WorkerExecutionRecoveryService to find pending scheduled workers on startup.
   */
  Uni<List<EventLog>> findByTypes(Collection<CaseHubEventType> types);

  /**
   * Find events for a specific case matching any of the given types, ordered by seq ascending. Used
   * by WorkerExecutionRecoveryService to rebuild CaseContext state.
   */
  Uni<List<EventLog>> findByCaseAndTypes(UUID caseId, Collection<CaseHubEventType> types);

  /**
   * Find events for a specific case+worker of a specific type. Used by WorkerExecutionJobListener
   * to count failed attempts for retry logic.
   */
  Uni<List<EventLog>> findByCaseAndWorkerAndType(
      UUID caseId, String workerId, CaseHubEventType type);
}
