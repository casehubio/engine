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
package io.casehub.api.model.stigmergy;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConductorInboxEntry(
    UUID caseId,
    String id,
    ImprovementStage stage,
    Status status,
    @Nullable String category,
    @Nullable String areaId,
    @Nullable UUID improvementCaseId,
    @Nullable String summary,
    List<EscalationTrigger> escalationTriggers,
    double confidence,
    Instant queuedAt,
    @Nullable Instant resolvedAt,
    @Nullable Integer timeoutMinutes,
    @Nullable ConductorDecision decision) {

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
    REDIRECTED,
    TIMED_OUT,
    AUTO_APPROVED
  }
}
