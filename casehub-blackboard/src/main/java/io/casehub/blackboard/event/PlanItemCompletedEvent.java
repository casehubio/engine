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
package io.casehub.blackboard.event;

import java.util.UUID;

/**
 * CDI event fired after a {@link io.casehub.blackboard.plan.PlanItem} is marked COMPLETED in the
 * BlackboardRegistry. Fired via {@code Event.fireAsync()} from {@link
 * io.casehub.blackboard.handler.PlanItemCompletionHandler}.
 *
 * <p>By the time observers receive this event, the PlanItem status is already COMPLETED in the
 * registry — no polling required.
 *
 * @param caseId the case the PlanItem belongs to
 * @param planItemId the completed PlanItem's id
 * @param workerName the worker whose execution produced the completion
 */
public record PlanItemCompletedEvent(UUID caseId, String planItemId, String workerName) {}
