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
package io.casehub.engine.internal.history;

public enum CaseHubEventType {
  CASE_STARTED,
  CASE_COMPLETED,
  CASE_FAULTED,
  CASE_CANCELLED,
  CASE_STATUS_CHANGED,

  TASK_CREATED,
  TASK_COMPLETED,
  TASK_FAILED,
  TASK_CANCELLED,

  WORKER_SCHEDULED,
  WORKER_EXECUTION_STARTED,
  WORKER_EXECUTION_COMPLETED,
  WORKER_EXECUTION_FAILED,

  WORK_SUBMITTED, // orchestrated work submitted via WorkOrchestrator
  WORK_COMPLETED, // orchestrated work completed; case may resume from WAITING

  SIGNAL_RECEIVED,

  MILESTONE_REACHED,
  MILESTONE_ACTIVATED,
  MILESTONE_COMPLETED,
  MILESTONE_SLA_VIOLATED,

  GOAL_REACHED,
}
