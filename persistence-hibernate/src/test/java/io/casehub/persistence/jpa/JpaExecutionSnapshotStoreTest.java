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
package io.casehub.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStoreContractTest;
import io.casehub.engine.plan.execution.DagResultSnapshot;
import io.casehub.engine.plan.snapshot.DagPlanSnapshot;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@QuarkusTest
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class JpaExecutionSnapshotStoreTest extends ExecutionSnapshotStoreContractTest {

  @Inject ExecutionSnapshotStore injectedStore;

  @Override
  protected ExecutionSnapshotStore store() {
    return injectedStore;
  }

  @Override
  protected String tenancyId() {
    return "test-tenant";
  }

  @Test
  void upsertOverwritesSingleColumn() {
    UUID caseId = UUID.randomUUID();
    store().storeDagPlan(caseId, "test-tenant", new DagPlanSnapshot(Map.of(), Instant.now()));
    store()
        .storeDagResult(
            caseId,
            "test-tenant",
            new DagResultSnapshot(Map.of(), Map.of(), true, Duration.ofMillis(50), Instant.now()));

    assertThat(store().getDagPlan(caseId, "test-tenant")).isPresent();
    assertThat(store().getDagResult(caseId, "test-tenant")).isPresent();
  }

  @Test
  void tenantIsolation() {
    UUID caseId = UUID.randomUUID();
    store().storeDagPlan(caseId, "tenant-a", new DagPlanSnapshot(Map.of(), Instant.now()));

    assertThat(store().getDagPlan(caseId, "tenant-a")).isPresent();
    assertThat(store().getDagPlan(caseId, "tenant-b")).isEmpty();
  }
}
