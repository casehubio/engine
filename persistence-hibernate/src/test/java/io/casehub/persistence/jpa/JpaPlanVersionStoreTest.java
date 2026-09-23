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

import io.casehub.engine.common.spi.recovery.PlanVersionStore;
import io.casehub.engine.common.spi.recovery.PlanVersionStoreContractTest;
import io.casehub.engine.plan.execution.PlanVersion;
import io.casehub.engine.plan.snapshot.PlanVersionDelta;
import io.casehub.engine.plan.snapshot.PlanVersionTrigger;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@QuarkusTest
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class JpaPlanVersionStoreTest extends PlanVersionStoreContractTest {

  @Inject PlanVersionStore injectedStore;

  @Override
  protected PlanVersionStore store() {
    return injectedStore;
  }

  @Override
  protected String tenancyId() {
    return "test-tenant";
  }

  @Test
  void tenantIsolation() {
    UUID caseId = UUID.randomUUID();
    PlanVersion v =
        new PlanVersion(
            1,
            caseId,
            Instant.now(),
            new PlanVersionTrigger.InitialDecomposition("goal", "llm"),
            null,
            new PlanVersionDelta(List.of("step-1"), List.of(), List.of("compound-1"), Map.of()));
    store().store(v, "tenant-a");

    assertThat(store().getHistory(caseId, "tenant-a")).hasSize(1);
    assertThat(store().getHistory(caseId, "tenant-b")).isEmpty();
  }

  @Test
  void triggerSerializationRoundTrip() {
    UUID caseId = UUID.randomUUID();
    var trigger =
        new PlanVersionTrigger.CompoundAdaptation(
            "compound-1", "binding-a", io.casehub.api.model.RecoveryLevel.REASONING, "test");
    var delta =
        new PlanVersionDelta(
            List.of("step-1", "step-2"), List.of("old-1"), List.of("compound-1"), Map.of());
    var pv = new PlanVersion(1, caseId, Instant.now(), trigger, null, delta);
    store().store(pv, "test-tenant");

    var retrieved = store().getVersion(caseId, 1, "test-tenant");
    assertThat(retrieved).isPresent();
    PlanVersion rv = retrieved.get();
    assertThat(rv.trigger()).isInstanceOf(PlanVersionTrigger.CompoundAdaptation.class);
    var rt = (PlanVersionTrigger.CompoundAdaptation) rv.trigger();
    assertThat(rt.compoundId()).isEqualTo("compound-1");
    assertThat(rt.bindingName()).isEqualTo("binding-a");
    assertThat(rt.level()).isEqualTo(io.casehub.api.model.RecoveryLevel.REASONING);
    assertThat(rv.delta().materializedStepIds()).containsExactly("step-1", "step-2");
    assertThat(rv.delta().obsoletedStepIds()).containsExactly("old-1");
  }
}
