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
package io.casehub.engine.internal.improvement;

import io.casehub.api.model.stigmergy.ConductorDecision;
import io.casehub.api.model.stigmergy.ConductorInboxEntry;
import io.casehub.api.model.stigmergy.SummaryScope;
import io.casehub.api.model.stigmergy.TickTrace;
import io.casehub.api.model.stigmergy.WatchPattern;
import io.casehub.api.spi.improvement.SummarizationProvider;
import io.casehub.api.view.DenyPatternView;
import io.casehub.api.view.EvolutionSummary;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DefaultEngineEvolutionApi {

  private final TickTraceBuffer tickTraceBuffer;
  private final ImprovementBudgetEnforcer budgetEnforcer;
  private final ConductorInboxManager inboxManager;
  private final SummarizationProvider summarizationProvider;
  private final ImprovementCoordinator coordinator;

  public DefaultEngineEvolutionApi(
      TickTraceBuffer tickTraceBuffer,
      ImprovementBudgetEnforcer budgetEnforcer,
      ConductorInboxManager inboxManager,
      SummarizationProvider summarizationProvider,
      ImprovementCoordinator coordinator) {
    this.tickTraceBuffer = tickTraceBuffer;
    this.budgetEnforcer = budgetEnforcer;
    this.inboxManager = inboxManager;
    this.summarizationProvider = summarizationProvider;
    this.coordinator = coordinator;
  }

  public List<TickTrace> getTickHistory(UUID caseId, @Nullable Integer limit) {
    return tickTraceBuffer.recent(caseId, limit != null ? limit : 20);
  }

  public DenyPatternView getDenyPatterns(UUID caseId, String tenancyId) {
    return new DenyPatternView(
            List.copyOf(ImprovementBudgetEnforcer.staticDenyPatterns()),
            budgetEnforcer.dynamicDenyPatterns(caseId, tenancyId).stream()
                          .map(p -> new DenyPatternView.DynamicDenyEntry(p, "operator", Instant.now()))
                          .toList());
  }

  public EvolutionSummary getSummary(
      UUID caseId,
      String tenancyId,
      @Nullable String areaId,
      @Nullable String category,
      @Nullable Integer timeWindowMinutes,
      @Nullable UUID improvementCaseId) {
    var scope = new SummaryScope(areaId, category, timeWindowMinutes, improvementCaseId);
    return summarizationProvider.summarize(caseId, tenancyId, scope);
  }

  public void addDenyPattern(UUID caseId, String tenancyId, String pattern) {
    budgetEnforcer.addDenyPattern(caseId, pattern, tenancyId);
  }

  public void removeDenyPattern(UUID caseId, String tenancyId, String pattern) {
    budgetEnforcer.removeDenyPattern(caseId, pattern, tenancyId);
  }

  public List<ConductorInboxEntry> getInbox(UUID caseId, String tenancyId) {
    return inboxManager.pending(caseId, tenancyId);
  }

  public void resolveGate(
          UUID caseId,
          String tenancyId,
          String entryId,
          ConductorInboxEntry.Status outcome,
          @Nullable String reason,
          @Nullable String feedback) {
    var decision = new ConductorDecision(outcome, null, reason, feedback);
    inboxManager.resolve(caseId, entryId, decision, tenancyId);
  }

  public void addWatchPattern(
          UUID caseId,
          String tenancyId,
          @Nullable String category,
          @Nullable String areaId,
          @Nullable String targetPattern,
          @Nullable Integer minEstimatedSize) {
    var pattern =
            new WatchPattern(
                    UUID.randomUUID().toString(),
                    category,
                    areaId,
                    targetPattern,
                    minEstimatedSize,
                    Instant.now());
    inboxManager.addWatchPattern(caseId, pattern, tenancyId);
  }

  public void removeWatchPattern(UUID caseId, String tenancyId, String patternId) {
    inboxManager.removeWatchPattern(caseId, patternId, tenancyId);
  }

    public void blockImprovement(UUID caseId, String tenancyId, UUID improvementId, UUID blockedBy) {
        coordinator.block(caseId, improvementId, blockedBy, tenancyId);
    }

  public void unblockImprovement(UUID caseId, String tenancyId, UUID improvementId) {
    coordinator.unblock(caseId, improvementId, tenancyId);
  }
}
