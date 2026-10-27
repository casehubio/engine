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
package io.casehub.engine.planning.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.PlanExecutionContext;
import io.casehub.api.model.Binding;
import io.casehub.api.model.BindingRequires;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.engine.planning.plan.DefaultCasePlanModel;
import io.casehub.platform.api.identity.TenancyConstants;
import io.casehub.worker.api.Capability;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequiresBlackboardPlanConfigurerTest {

  private PlanExecutionContext ctx(CaseDefinition def) {
    return new PlanExecutionContext(
        UUID.randomUUID(),
        def,
        mock(CaseContext.class),
        CaseStatus.RUNNING,
        TenancyConstants.DEFAULT_TENANT_ID,
        List.of(),
        null,
        null);
  }

  @Test
  void registersRequiresMetadataFromBindings() {
    var binding =
        Binding.builder()
            .name("aggregate")
            .capability(Capability.of("agg", ".", "."))
            .on(new io.casehub.api.model.ContextChangeTrigger(".go"))
            .requires(new BindingRequires(List.of("a", "b"), 1, OnThresholdReached.KEEP))
            .build();

    var def =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0.0")
            .binding(binding)
            .build();

    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    new RequiresBlackboardPlanConfigurer().configure(plan, ctx(def));

    var metadata = plan.getRequiresMetadata();
    assertThat(metadata).hasSize(1);
    assertThat(metadata.get("aggregate")).isNotNull();
    assertThat(metadata.get("aggregate").bindings()).containsExactly("a", "b");
  }

  @Test
  void thresholdTriggeredFlag() {
    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    assertThat(plan.isRequiresThresholdTriggered("x")).isFalse();
    plan.markRequiresThresholdTriggered("x");
    assertThat(plan.isRequiresThresholdTriggered("x")).isTrue();
  }

  @Test
  void skipsBindingsWithoutRequires() {
    var binding =
        Binding.builder()
            .name("plain")
            .capability(Capability.of("cap", ".", "."))
            .on(new io.casehub.api.model.ContextChangeTrigger(".go"))
            .build();

    var def =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0.0")
            .binding(binding)
            .build();

    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    new RequiresBlackboardPlanConfigurer().configure(plan, ctx(def));

    assertThat(plan.getRequiresMetadata()).isEmpty();
  }
}
