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

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.stigmergy.ConductorInboxEntry;
import io.casehub.api.model.stigmergy.ConductorInboxEntry.Status;
import io.casehub.api.model.stigmergy.ImprovementStage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvolutionApiTest {

  private DefaultEngineEvolutionApi api;
  private ConductorInboxManager inboxManager;
  private ImprovementCoordinator coordinator;
  private UUID caseId;
  private ImprovementCategoryTracker categoryTracker;
  private ImprovementCircuitBreaker circuitBreaker;
  private ReadinessValidator readinessValidator;

  @BeforeEach
  void setUp() {
    var budgetEnforcer = new ImprovementBudgetEnforcer(new InMemoryDenyPatternStore());
    inboxManager =
        new ConductorInboxManager(
            new InMemoryConductorInboxRepository(), new InMemoryWatchPatternStore());
    coordinator = new ImprovementCoordinator(new InMemoryImprovementBlockStore());
    categoryTracker = new ImprovementCategoryTracker();
    circuitBreaker = new ImprovementCircuitBreaker(new TestEvent<>());
    var areaRegistry = new CapabilityAreaRegistry();
    readinessValidator =
        new ReadinessValidator(
            areaRegistry, new DefaultComplianceChecklistProvider(), new TestEvent<>());

    api =
        new DefaultEngineEvolutionApi(
            new TickTraceBuffer(),
            budgetEnforcer,
            inboxManager,
            new DefaultSummarizationProvider(),
            coordinator,
            categoryTracker,
            circuitBreaker,
            readinessValidator);

    caseId = UUID.randomUUID();
  }

  @Test
  void addAndRemoveDenyPattern() {
    api.addDenyPattern(caseId, "test-tenant", "blocked-path");

    var view = api.getDenyPatterns(caseId, "test-tenant");
    assertThat(view.staticPatterns()).isNotEmpty();
    assertThat(view.dynamicPatterns()).hasSize(1);
    assertThat(view.dynamicPatterns().get(0).pattern()).isEqualTo("blocked-path");

    api.removeDenyPattern(caseId, "test-tenant", "blocked-path");
    var viewAfter = api.getDenyPatterns(caseId, "test-tenant");
    assertThat(viewAfter.dynamicPatterns()).isEmpty();
  }

  @Test
  void getInboxReturnsPendingEntries() {
    var entry =
        new ConductorInboxEntry(
            caseId,
            "e1",
            ImprovementStage.RESEARCH_SCOPE,
            Status.PENDING,
            null,
            null,
            null,
            "test",
            List.of(),
            0.8,
            Instant.now(),
            null,
            null,
            null);
    inboxManager.enqueue(caseId, entry, "test-tenant");

    var inbox = api.getInbox(caseId, "test-tenant");
    assertThat(inbox).hasSize(1);
    assertThat(inbox.get(0).id()).isEqualTo("e1");
  }

  @Test
  void resolveGateUpdatesEntryStatus() {
    var entry =
        new ConductorInboxEntry(
            caseId,
            "e1",
            ImprovementStage.RESEARCH_SCOPE,
            Status.PENDING,
            null,
            null,
            null,
            "test",
            List.of(),
            0.8,
            Instant.now(),
            null,
            null,
            null);
    inboxManager.enqueue(caseId, entry, "test-tenant");

    api.resolveGate(caseId, "test-tenant", "e1", Status.APPROVED, "looks good", null);

    assertThat(api.getInbox(caseId, "test-tenant")).isEmpty();
  }

  @Test
  void getSummaryReturnsStructuredSummary() {
    var summary = api.getSummary(caseId, "t1", null, null, null, null);

    assertThat(summary).isNotNull();
    assertThat(summary.computedAt()).isNotNull();
  }

  @Test
  void addAndRemoveWatchPattern() {
    api.addWatchPattern(caseId, "test-tenant", "security", null, null, null);

    var patterns = inboxManager.activeWatchPatterns(caseId, "test-tenant");
    assertThat(patterns).hasSize(1);
    assertThat(patterns.get(0).category()).isEqualTo("security");

    api.removeWatchPattern(caseId, "test-tenant", patterns.get(0).id());
    assertThat(inboxManager.activeWatchPatterns(caseId, "test-tenant")).isEmpty();
  }

  @Test
  void blockAndUnblockImprovement() {
    var improvementId = UUID.randomUUID();
    var blockerId = UUID.randomUUID();

    api.blockImprovement(caseId, "test-tenant", improvementId, blockerId);
    assertThat(coordinator.isBlocked(caseId, improvementId, "test-tenant")).isTrue();

    api.unblockImprovement(caseId, "test-tenant", improvementId);
    assertThat(coordinator.isBlocked(caseId, improvementId, "test-tenant")).isFalse();
  }

  @Test
  void getTickHistoryDefaultLimit() {
    var history = api.getTickHistory(caseId, null);
    assertThat(history).isEmpty();
  }

  @Test
  void pauseCategoryDelegatesToTracker() {
    api.pauseCategory(caseId, "test-tenant", "security", 60);
    assertThat(categoryTracker.isSuppressed(caseId, "security")).isTrue();
  }

  @Test
  void unpauseCategoryDelegatesToTracker() {
    categoryTracker.pauseCategory(caseId, "security", java.time.Duration.ofMinutes(60));
    assertThat(categoryTracker.isSuppressed(caseId, "security")).isTrue();

    api.unpauseCategory(caseId, "test-tenant", "security");
    assertThat(categoryTracker.isSuppressed(caseId, "security")).isFalse();
  }

  @Test
  void resetCircuitBreakerDelegatesToBreaker() {
    var healthTracker = new HealthScoreTracker(new CapabilityAreaRegistry());
    var policy =
        new io.casehub.api.model.stigmergy.HealthPolicy(null, null, null, null, null, null);
    circuitBreaker.evaluate(caseId, "test-tenant", healthTracker, policy);
    circuitBreaker.evaluate(caseId, "test-tenant", healthTracker, policy);

    api.resetCircuitBreaker(caseId);
    assertThat(circuitBreaker.state(caseId))
        .isEqualTo(io.casehub.api.model.stigmergy.CircuitBreakerState.CLOSED);
  }

  @Test
  void getReadinessReportDelegatesToValidator() {
    var config =
        new io.casehub.api.model.stigmergy.ImprovementConfig(
            null, null, null, null, null, null, null, null, null, null, null);
    var report =
        api.getReadinessReport(
            caseId,
            "test-tenant",
            io.casehub.api.model.stigmergy.ComplianceLevel.L1_OBSERVE,
            config);
    assertThat(report).isNotNull();
    assertThat(report.targetLevel())
        .isEqualTo(io.casehub.api.model.stigmergy.ComplianceLevel.L1_OBSERVE);
    assertThat(report.evaluatedAt()).isNotNull();
  }

  @Test
  void triggerReadinessValidationReturnsReport() {
    var config =
        new io.casehub.api.model.stigmergy.ImprovementConfig(
            null, null, null, null, null, null, null, null, null, null, null);
    var report =
        api.triggerReadinessValidation(
            caseId, "test-tenant", io.casehub.api.model.stigmergy.ComplianceLevel.L0_INERT, config);
    assertThat(report).isNotNull();
    assertThat(report.passed()).isTrue();
  }
}
