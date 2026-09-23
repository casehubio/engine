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
package io.casehub.engine.common.spi.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.plan.JoinType;
import io.casehub.engine.plan.execution.DagResultSnapshot;
import io.casehub.engine.plan.snapshot.DagNodeSnapshot;
import io.casehub.engine.plan.snapshot.DagPlanSnapshot;
import io.casehub.engine.plan.snapshot.DecompositionSnapshot;
import io.casehub.engine.plan.snapshot.LeafTaskSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public abstract class ExecutionSnapshotStoreContractTest {

  protected abstract ExecutionSnapshotStore store();

  protected abstract String tenancyId();

  @Test
  void storeAndRetrieveDecomposition() {
    UUID caseId = UUID.randomUUID();
    var snapshot =
        new DecompositionSnapshot(new LeafTaskSnapshot("l1", "desc", null), Instant.now());

    store().storeDecomposition(caseId, tenancyId(), snapshot);

    assertThat(store().getDecomposition(caseId, tenancyId())).contains(snapshot);
  }

  @Test
  void storeAndRetrieveDagPlan() {
    UUID caseId = UUID.randomUUID();
    var snapshot = new DagPlanSnapshot(Map.of(), Instant.now());

    store().storeDagPlan(caseId, tenancyId(), snapshot);

    assertThat(store().getDagPlan(caseId, tenancyId())).contains(snapshot);
  }

  @Test
  void storeAndRetrieveDagResult() {
    UUID caseId = UUID.randomUUID();
    var snapshot =
        new DagResultSnapshot(Map.of(), Map.of(), true, Duration.ofSeconds(1), Instant.now());

    store().storeDagResult(caseId, tenancyId(), snapshot);

    assertThat(store().getDagResult(caseId, tenancyId())).contains(snapshot);
  }

  @Test
  void evictRemovesAllSnapshots() {
    UUID caseId = UUID.randomUUID();
    store()
        .storeDecomposition(
            caseId,
            tenancyId(),
            new DecompositionSnapshot(new LeafTaskSnapshot("l1", null, null), Instant.now()));
    store().storeDagPlan(caseId, tenancyId(), new DagPlanSnapshot(Map.of(), Instant.now()));

    store().evict(caseId);

    assertThat(store().getDecomposition(caseId, tenancyId())).isEmpty();
    assertThat(store().getDagPlan(caseId, tenancyId())).isEmpty();
  }

  @Test
  void getReturnsEmptyForUnknownCase() {
    assertThat(store().getDecomposition(UUID.randomUUID(), tenancyId())).isEmpty();
    assertThat(store().getDagPlan(UUID.randomUUID(), tenancyId())).isEmpty();
    assertThat(store().getDagResult(UUID.randomUUID(), tenancyId())).isEmpty();
  }

  @Test
  void storeOverwritesPreviousSnapshot() {
    UUID caseId = UUID.randomUUID();
    var first = new DagPlanSnapshot(Map.of(), Instant.now());
    var second =
        new DagPlanSnapshot(
            Map.of("n1", new DagNodeSnapshot("n1", "t1", "d", "e", Set.of(), JoinType.ALL_OF)),
            Instant.now());

    store().storeDagPlan(caseId, tenancyId(), first);
    store().storeDagPlan(caseId, tenancyId(), second);

    assertThat(store().getDagPlan(caseId, tenancyId())).contains(second);
    assertThat(store().getDagPlan(caseId, tenancyId()).get().nodes()).hasSize(1);
  }
}
