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
package io.casehub.api.view;

import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.stigmergy.SummaryScope;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record EvolutionSummary(
    SummaryScope scope,
    Instant computedAt,
    int totalImprovements,
    int successCount,
    int failureCount,
    int rejectionCount,
    int regressionCount,
    int rollbackCount,
    @Nullable Double healthTrend,
    @Nullable Double successRateTrend,
    List<CategorySummary> categories,
    List<ResearchDirectionSummary> researchDirections,
    List<NotableEvent> notableEvents) {

  public record CategorySummary(
      String category, int totalOutcomes, double successRate, boolean suppressed, boolean paused) {}

  public record ResearchDirectionSummary(
      String areaId,
      int hypothesesGenerated,
      int hypothesesApproved,
      int hypothesesRejected,
      @Nullable String currentFocus,
      List<String> activeHypotheses) {}

  public record NotableEvent(
      CaseHubEventType type, Instant timestamp, Map<String, String> summary) {}
}
