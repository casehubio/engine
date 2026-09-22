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

import io.casehub.api.model.stigmergy.ConductorDecision;
import io.casehub.api.model.stigmergy.ConductorInboxEntry;
import io.casehub.api.model.stigmergy.ConductorInboxEntry.Status;
import io.casehub.api.model.stigmergy.ImprovementStage;
import io.casehub.api.model.stigmergy.WatchPattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConductorInboxManagerTest {

  private ConductorInboxManager manager;
  private UUID caseId;

  @BeforeEach
  void setUp() {
    manager = new ConductorInboxManager();
    caseId = UUID.randomUUID();
  }

  @Test
  void enqueueAndRetrievePending() {
    var entry = makeEntry("e1", ImprovementStage.RESEARCH_SCOPE);
    manager.enqueue(caseId, entry);

    var pending = manager.pending(caseId);
    assertThat(pending).hasSize(1);
    assertThat(pending.get(0).id()).isEqualTo("e1");
    assertThat(pending.get(0).status()).isEqualTo(Status.PENDING);
  }

  @Test
  void resolveGateApproved() {
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.RESEARCH_SCOPE));
    var decision = new ConductorDecision(Status.APPROVED, null, "looks good", null);

    manager.resolve(caseId, "e1", decision);

    assertThat(manager.pending(caseId)).isEmpty();
    assertThat(manager.pendingCount(caseId)).isZero();
  }

  @Test
  void resolveGateRejected() {
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.HYPOTHESIS_APPROVAL));
    var decision = new ConductorDecision(Status.REJECTED, null, "not viable", null);

    manager.resolve(caseId, "e1", decision);

    assertThat(manager.pending(caseId)).isEmpty();
  }

  @Test
  void resolveGateRedirected() {
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.IMPLEMENTATION_PLAN));
    var decision = new ConductorDecision(Status.REDIRECTED, null, "try different approach", null);

    manager.resolve(caseId, "e1", decision);

    assertThat(manager.pending(caseId)).isEmpty();
  }

  @Test
  void pendingCountMatchesPendingEntries() {
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.RESEARCH_SCOPE));
    manager.enqueue(caseId, makeEntry("e2", ImprovementStage.HYPOTHESIS_APPROVAL));

    assertThat(manager.pendingCount(caseId)).isEqualTo(2);

    manager.resolve(caseId, "e1", new ConductorDecision(Status.APPROVED, null, null, null));

    assertThat(manager.pendingCount(caseId)).isEqualTo(1);
  }

  @Test
  void perCaseIsolation() {
    var case2 = UUID.randomUUID();
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.RESEARCH_SCOPE));
    manager.enqueue(case2, makeEntry("e2", ImprovementStage.RESEARCH_SCOPE));

    assertThat(manager.pending(caseId)).hasSize(1);
    assertThat(manager.pending(case2)).hasSize(1);
    assertThat(manager.pending(caseId).get(0).id()).isEqualTo("e1");
    assertThat(manager.pending(case2).get(0).id()).isEqualTo("e2");
  }

  @Test
  void resolveNonexistentEntryIsNoOp() {
    manager.resolve(
        caseId, "nonexistent", new ConductorDecision(Status.APPROVED, null, null, null));

    assertThat(manager.pending(caseId)).isEmpty();
  }

  @Test
  void resolvedEntryRetainsDecision() {
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.PR_REVIEW));
    var decision = new ConductorDecision(Status.APPROVED, null, "LGTM", "nice work");

    manager.resolve(caseId, "e1", decision);

    var resolved =
        manager.allEntries(caseId).stream()
            .filter(e -> e.id().equals("e1"))
            .findFirst()
            .orElseThrow();
    assertThat(resolved.status()).isEqualTo(Status.APPROVED);
    assertThat(resolved.decision()).isEqualTo(decision);
    assertThat(resolved.resolvedAt()).isNotNull();
  }

  @Test
  void watchPatternAddAndList() {
    var pattern = new WatchPattern("w1", "security", null, null, null, Instant.now());
    manager.addWatchPattern(caseId, pattern);

    assertThat(manager.activeWatchPatterns(caseId)).hasSize(1);
    assertThat(manager.activeWatchPatterns(caseId).get(0).id()).isEqualTo("w1");
  }

  @Test
  void watchPatternRemove() {
    manager.addWatchPattern(
        caseId, new WatchPattern("w1", "security", null, null, null, Instant.now()));
    manager.addWatchPattern(
        caseId, new WatchPattern("w2", "architecture", null, null, null, Instant.now()));

    manager.removeWatchPattern(caseId, "w1");

    assertThat(manager.activeWatchPatterns(caseId)).hasSize(1);
    assertThat(manager.activeWatchPatterns(caseId).get(0).id()).isEqualTo("w2");
  }

  @Test
  void watchPatternPerCaseIsolation() {
    var case2 = UUID.randomUUID();
    manager.addWatchPattern(
        caseId, new WatchPattern("w1", "security", null, null, null, Instant.now()));

    assertThat(manager.activeWatchPatterns(caseId)).hasSize(1);
    assertThat(manager.activeWatchPatterns(case2)).isEmpty();
  }

  @Test
  void resetClearsAllState() {
    manager.enqueue(caseId, makeEntry("e1", ImprovementStage.RESEARCH_SCOPE));
    manager.addWatchPattern(
        caseId, new WatchPattern("w1", "security", null, null, null, Instant.now()));

    manager.reset();

    assertThat(manager.pending(caseId)).isEmpty();
    assertThat(manager.activeWatchPatterns(caseId)).isEmpty();
  }

  @Test
  void emptyPendingForUnknownCase() {
    assertThat(manager.pending(UUID.randomUUID())).isEmpty();
    assertThat(manager.pendingCount(UUID.randomUUID())).isZero();
  }

  private ConductorInboxEntry makeEntry(String id, ImprovementStage stage) {
    return new ConductorInboxEntry(
        id,
        stage,
        Status.PENDING,
        null,
        null,
        null,
        "test entry",
        List.of(),
        0.8,
        Instant.now(),
        null,
        null,
        null);
  }
}
