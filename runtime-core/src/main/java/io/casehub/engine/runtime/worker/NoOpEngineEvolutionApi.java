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
package io.casehub.engine.runtime.worker;

import io.casehub.api.model.improvement.ComplianceLevel;
import io.casehub.api.model.improvement.ImprovementConfig;
import io.casehub.api.model.improvement.ReadinessReport;
import io.casehub.api.model.stigmergy.ArtifactManifest;
import io.casehub.api.model.stigmergy.CategoryDescriptor;
import io.casehub.api.model.stigmergy.ConductorInboxEntry;
import io.casehub.api.model.stigmergy.GatePolicy;
import io.casehub.api.model.stigmergy.StageDescriptor;
import io.casehub.api.model.stigmergy.TickTrace;
import io.casehub.api.model.stigmergy.WatchPattern;
import io.casehub.api.spi.improvement.EngineEvolutionApi;
import io.casehub.api.view.DenyPatternView;
import io.casehub.api.view.EvolutionStateSnapshot;
import io.casehub.api.view.EvolutionSummary;
import io.casehub.api.view.ResearchCorpusView;
import java.util.List;
import java.util.UUID;

public class NoOpEngineEvolutionApi implements EngineEvolutionApi {

  private static UnsupportedOperationException unavailable() {
    return new UnsupportedOperationException("Evolution subsystem not available");
  }

  @Override
  public List<TickTrace> getTickHistory(UUID caseId, Integer limit) {
    throw unavailable();
  }

  @Override
  public DenyPatternView getDenyPatterns(UUID caseId, String tenancyId) {
    throw unavailable();
  }

  @Override
  public EvolutionSummary getSummary(
      UUID caseId,
      String tenancyId,
      String areaId,
      String category,
      Integer timeWindowMinutes,
      UUID improvementCaseId) {
    throw unavailable();
  }

  @Override
  public void addDenyPattern(UUID caseId, String tenancyId, String pattern) {
    throw unavailable();
  }

  @Override
  public void removeDenyPattern(UUID caseId, String tenancyId, String pattern) {
    throw unavailable();
  }

  @Override
  public List<ConductorInboxEntry> getInbox(UUID caseId, String tenancyId) {
    throw unavailable();
  }

  @Override
  public void resolveGate(
      UUID caseId,
      String tenancyId,
      String entryId,
      ConductorInboxEntry.Status outcome,
      String reason,
      String feedback) {
    throw unavailable();
  }

  @Override
  public void addWatchPattern(
      UUID caseId,
      String tenancyId,
      String category,
      String areaId,
      String targetPattern,
      Integer minEstimatedSize) {
    throw unavailable();
  }

  @Override
  public void removeWatchPattern(UUID caseId, String tenancyId, String patternId) {
    throw unavailable();
  }

  @Override
  public void blockImprovement(UUID caseId, String tenancyId, UUID improvementId, UUID blockedBy) {
    throw unavailable();
  }

  @Override
  public void unblockImprovement(UUID caseId, String tenancyId, UUID improvementId) {
    throw unavailable();
  }

  @Override
  public void pauseCategory(UUID caseId, String tenancyId, String category, int durationMinutes) {
    throw unavailable();
  }

  @Override
  public void unpauseCategory(UUID caseId, String tenancyId, String category) {
    throw unavailable();
  }

  @Override
  public void resetCircuitBreaker(UUID caseId) {
    throw unavailable();
  }

  @Override
  public ReadinessReport getReadinessReport(
      UUID caseId, String tenancyId, ComplianceLevel targetLevel, ImprovementConfig config) {
    throw unavailable();
  }

  @Override
  public ReadinessReport triggerReadinessValidation(
      UUID caseId, String tenancyId, ComplianceLevel targetLevel, ImprovementConfig config) {
    throw unavailable();
  }

  @Override
  public EvolutionStateSnapshot getEvolutionState(
      UUID caseId, String tenancyId, ImprovementConfig config) {
    throw unavailable();
  }

  @Override
  public ResearchCorpusView getResearchCorpus(String query, String areaId, int limit) {
    throw unavailable();
  }

  @Override
  public void setGatePolicy(UUID caseId, String tenancyId, GatePolicy policy) {
    throw unavailable();
  }

  @Override
  public List<WatchPattern> getWatchPatterns(UUID caseId, String tenancyId) {
    throw unavailable();
  }

  @Override
  public GatePolicy getGatePolicy(UUID caseId, String tenancyId) {
    throw unavailable();
  }

  @Override
  public List<StageDescriptor> getStages(UUID caseId) {
    throw unavailable();
  }

  @Override
  public List<CategoryDescriptor> getCategories(UUID caseId) {
    throw unavailable();
  }

  @Override
  public ArtifactManifest getArtifactTrail(UUID caseId, String tenancyId, UUID improvementCaseId) {
    throw unavailable();
  }

  @Override
  public List<EvolutionStateSnapshot.ImprovementStreamView> getStreamProgress(
      UUID caseId, String tenancyId) {
    throw unavailable();
  }
}
