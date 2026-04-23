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
package io.casehub.api.spi;

import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkerContext;

/**
 * Builds startup context for a new worker.
 *
 * <p>Implementations query {@code CaseLedgerEntryRepository} (not EventLog) for prior worker
 * history, constructing {@link io.casehub.api.model.WorkerSummary} entries with {@code
 * ledgerEntryId} populated so new workers can set {@code causedByEntryId} on their own ledger
 * entries.
 */
public interface WorkerContextProvider {

  /**
   * Build context for a worker about to start work on a task.
   *
   * @param workerId the ID of the worker being started
   * @param task the work request describing what the worker should do
   * @return startup context including task description, channel, and lineage
   */
  WorkerContext buildContext(String workerId, WorkRequest task);
}
