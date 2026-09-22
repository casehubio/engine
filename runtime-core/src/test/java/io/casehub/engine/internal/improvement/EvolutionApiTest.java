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

  @BeforeEach
  void setUp() {
    var budgetEnforcer = new ImprovementBudgetEnforcer();
    inboxManager = new ConductorInboxManager();
    coordinator = new ImprovementCoordinator(new InMemoryImprovementBlockStore());

    api =
        new DefaultEngineEvolutionApi(
            new TickTraceBuffer(),
            budgetEnforcer,
            inboxManager,
            new DefaultSummarizationProvider(),
            coordinator);

    caseId = UUID.randomUUID();
  }

  @Test
  void addAndRemoveDenyPattern() {
    api.addDenyPattern(caseId, "blocked-path");

    var view = api.getDenyPatterns(caseId);
    assertThat(view.staticPatterns()).isNotEmpty();
    assertThat(view.dynamicPatterns()).hasSize(1);
    assertThat(view.dynamicPatterns().get(0).pattern()).isEqualTo("blocked-path");

    api.removeDenyPattern(caseId, "blocked-path");
    var viewAfter = api.getDenyPatterns(caseId);
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
    inboxManager.enqueue(caseId, entry);

    var inbox = api.getInbox(caseId);
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
    inboxManager.enqueue(caseId, entry);

    api.resolveGate(caseId, "e1", Status.APPROVED, "looks good", null);

    assertThat(api.getInbox(caseId)).isEmpty();
  }

  @Test
  void getSummaryReturnsStructuredSummary() {
    var summary = api.getSummary(caseId, "t1", null, null, null, null);

    assertThat(summary).isNotNull();
    assertThat(summary.computedAt()).isNotNull();
  }

  @Test
  void addAndRemoveWatchPattern() {
    api.addWatchPattern(caseId, "security", null, null, null);

    var patterns = inboxManager.activeWatchPatterns(caseId);
    assertThat(patterns).hasSize(1);
    assertThat(patterns.get(0).category()).isEqualTo("security");

    api.removeWatchPattern(caseId, patterns.get(0).id());
    assertThat(inboxManager.activeWatchPatterns(caseId)).isEmpty();
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
}
