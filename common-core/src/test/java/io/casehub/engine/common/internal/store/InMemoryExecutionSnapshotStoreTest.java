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
package io.casehub.engine.common.internal.store;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStoreContractTest;
import io.casehub.engine.plan.snapshot.DecompositionSnapshot;
import io.casehub.engine.plan.snapshot.LeafTaskSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemoryExecutionSnapshotStoreTest extends ExecutionSnapshotStoreContractTest {

  private final InMemoryExecutionSnapshotStore store = new InMemoryExecutionSnapshotStore();

  @Override
  protected ExecutionSnapshotStore store() {
    return store;
  }

  @Override
  protected String tenancyId() {
    return "test-tenant";
  }

  @Test
  void ttlSweepEvictsExpiredEntries() {
    var shortTtlStore = new InMemoryExecutionSnapshotStore(Duration.ZERO, Duration.ZERO);
    UUID caseId = UUID.randomUUID();
    shortTtlStore.storeDecomposition(
        caseId,
        "t",
        new DecompositionSnapshot(new LeafTaskSnapshot("l1", null, null), Instant.now()));

    assertThat(shortTtlStore.size()).isEqualTo(1);

    UUID caseId2 = UUID.randomUUID();
    shortTtlStore.storeDecomposition(
        caseId2,
        "t",
        new DecompositionSnapshot(new LeafTaskSnapshot("l2", null, null), Instant.now()));

    assertThat(shortTtlStore.getDecomposition(caseId, "t")).isEmpty();
  }
}
