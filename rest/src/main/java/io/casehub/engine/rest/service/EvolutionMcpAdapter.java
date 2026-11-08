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
package io.casehub.engine.rest.service;

import io.casehub.api.acl.AclCheck;
import io.casehub.api.acl.EngineResourceTypes;
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
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.mcp.ContextParam;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.platform.api.mcp.PlatformQuery;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@McpDomain("engine/evolution")
public class EvolutionMcpAdapter {

  @Inject EngineEvolutionApi delegate;
  @Inject CaseService caseService;

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get evolution tick history for a case")
  public List<TickTrace> getTickHistory(@PathParam UUID caseId, @Nullable Integer limit) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getTickHistory(caseId, limit);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get deny patterns for a case")
  public DenyPatternView getDenyPatterns(
      @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getDenyPatterns(caseId, tenancyId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get evolution summary with optional scope filters")
  public EvolutionSummary getSummary(
      @PathParam UUID caseId,
      @ContextParam("tenancyId") String tenancyId,
      @Nullable String areaId,
      @Nullable String category,
      @Nullable Integer timeWindowMinutes,
      @Nullable UUID improvementCaseId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getSummary(
        caseId, tenancyId, areaId, category, timeWindowMinutes, improvementCaseId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get conductor inbox entries for a case")
  public List<ConductorInboxEntry> getInbox(
      @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getInbox(caseId, tenancyId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get evolution readiness report")
  public ReadinessReport getReadinessReport(
      @PathParam UUID caseId,
      @ContextParam("tenancyId") String tenancyId,
      ComplianceLevel targetLevel,
      ImprovementConfig config) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getReadinessReport(caseId, tenancyId, targetLevel, config);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get full evolution state snapshot")
  public EvolutionStateSnapshot getEvolutionState(
      @PathParam UUID caseId,
      @ContextParam("tenancyId") String tenancyId,
      ImprovementConfig config) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getEvolutionState(caseId, tenancyId, config);
  }

  @PlatformQuery("Search the research corpus")
  public ResearchCorpusView getResearchCorpus(String query, @Nullable String areaId, int limit) {
    return delegate.getResearchCorpus(query, areaId, limit);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get artifact trail for an improvement case")
  public ArtifactManifest getArtifactTrail(
      @PathParam UUID caseId,
      @ContextParam("tenancyId") String tenancyId,
      @PathParam UUID improvementCaseId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getArtifactTrail(caseId, tenancyId, improvementCaseId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get active improvement stream progress")
  public List<EvolutionStateSnapshot.ImprovementStreamView> getStreamProgress(
      @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getStreamProgress(caseId, tenancyId);
  }

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.WRITE,
            resourceIdParam = "caseId")
    @PlatformMutation("Add a deny pattern to block improvement categories. Returns updated deny patterns")
    public DenyPatternView addDenyPattern(
            @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId, String pattern) {
        caseService.requireCaseAccess(caseId, AclAction.WRITE);
        delegate.addDenyPattern(caseId, tenancyId, pattern);
        return delegate.getDenyPatterns(caseId, tenancyId);
    }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Remove a deny pattern. Returns updated deny patterns")
  public DenyPatternView removeDenyPattern(
          @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId, String pattern) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.removeDenyPattern(caseId, tenancyId, pattern);
    return delegate.getDenyPatterns(caseId, tenancyId);
  }

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.WRITE,
            resourceIdParam = "caseId")
    @PlatformMutation("Resolve a conductor gate decision. outcome: APPROVED, REJECTED. Returns updated inbox")
    public List<ConductorInboxEntry> resolveGate(
            @PathParam UUID caseId,
            @ContextParam("tenancyId") String tenancyId,
            String entryId,
            ConductorInboxEntry.Status outcome,
            @Nullable String reason,
            @Nullable String feedback) {
        caseService.requireCaseAccess(caseId, AclAction.WRITE);
        delegate.resolveGate(caseId, tenancyId, entryId, outcome, reason, feedback);
        return delegate.getInbox(caseId, tenancyId);
    }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Add an escalation watch pattern. Returns updated watch patterns list")
  public List<WatchPattern> addWatchPattern(
          @PathParam UUID caseId,
          @ContextParam("tenancyId") String tenancyId,
          @Nullable String category,
          @Nullable String areaId,
          @Nullable String targetPattern,
          @Nullable Integer minEstimatedSize) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.addWatchPattern(caseId, tenancyId, category, areaId, targetPattern, minEstimatedSize);
    return delegate.getWatchPatterns(caseId, tenancyId);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Remove an escalation watch pattern. Returns updated watch patterns list")
  public List<WatchPattern> removeWatchPattern(
          @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId, String patternId) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.removeWatchPattern(caseId, tenancyId, patternId);
    return delegate.getWatchPatterns(caseId, tenancyId);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Block an improvement stream. Returns updated stream progress")
  public List<EvolutionStateSnapshot.ImprovementStreamView> blockImprovement(
          @PathParam UUID caseId,
          @ContextParam("tenancyId") String tenancyId,
          @PathParam UUID improvementId,
          UUID blockedBy) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.blockImprovement(caseId, tenancyId, improvementId, blockedBy);
    return delegate.getStreamProgress(caseId, tenancyId);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Unblock a previously blocked improvement stream. Returns updated stream progress")
  public List<EvolutionStateSnapshot.ImprovementStreamView> unblockImprovement(
          @PathParam UUID caseId,
          @ContextParam("tenancyId") String tenancyId,
          @PathParam UUID improvementId) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.unblockImprovement(caseId, tenancyId, improvementId);
    return delegate.getStreamProgress(caseId, tenancyId);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Pause a category from receiving improvements for durationMinutes. Returns updated evolution summary")
  public EvolutionSummary pauseCategory(
          @PathParam UUID caseId,
          @ContextParam("tenancyId") String tenancyId,
          String category,
          int durationMinutes) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.pauseCategory(caseId, tenancyId, category, durationMinutes);
    return delegate.getSummary(caseId, tenancyId, null, category, null, null);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.WRITE,
          resourceIdParam = "caseId")
  @PlatformMutation("Unpause a previously paused category. Returns updated evolution summary")
  public EvolutionSummary unpauseCategory(
          @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId, String category) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    delegate.unpauseCategory(caseId, tenancyId, category);
    return delegate.getSummary(caseId, tenancyId, null, category, null, null);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.ADMIN,
          resourceIdParam = "caseId")
  @PlatformMutation("Reset the health circuit breaker. Returns updated evolution summary")
  public EvolutionSummary resetCircuitBreaker(
          @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.ADMIN);
    delegate.resetCircuitBreaker(caseId);
    return delegate.getSummary(caseId, tenancyId, null, null, null, null);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.WRITE,
      resourceIdParam = "caseId")
  @PlatformMutation("Trigger evolution readiness validation")
  public ReadinessReport triggerReadinessValidation(
      @PathParam UUID caseId,
      @ContextParam("tenancyId") String tenancyId,
      ComplianceLevel targetLevel,
      ImprovementConfig config) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    return delegate.triggerReadinessValidation(caseId, tenancyId, targetLevel, config);
  }

  @AclCheck(
          resourceType = EngineResourceTypes.CASE,
          action = AclAction.ADMIN,
          resourceIdParam = "caseId")
  @PlatformMutation("Set gate policy for evolution lifecycle gates. Returns confirmed policy")
  public GatePolicy setGatePolicy(
          @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId, GatePolicy policy) {
    caseService.requireCaseAccess(caseId, AclAction.ADMIN);
    delegate.setGatePolicy(caseId, tenancyId, policy);
    return delegate.getGatePolicy(caseId, tenancyId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get active watch patterns for a case")
  public List<WatchPattern> getWatchPatterns(
      @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getWatchPatterns(caseId, tenancyId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get gate policy for a case")
  public GatePolicy getGatePolicy(
      @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getGatePolicy(caseId, tenancyId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get domain-contributed stage descriptors")
  public List<StageDescriptor> getStages(@PathParam UUID caseId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getStages(caseId);
  }

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.READ,
      resourceIdParam = "caseId")
  @PlatformQuery("Get improvement category descriptors")
  public List<CategoryDescriptor> getCategories(@PathParam UUID caseId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    return delegate.getCategories(caseId);
  }
}
