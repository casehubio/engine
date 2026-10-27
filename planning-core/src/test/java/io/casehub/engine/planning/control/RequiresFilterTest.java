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

import io.casehub.api.model.Binding;
import io.casehub.api.model.BindingRequires;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.engine.planning.plan.DefaultCasePlanModel;
import io.casehub.engine.planning.plan.PlanItem;
import io.casehub.worker.api.Capability;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequiresFilterTest {

  private Binding binding(String name, BindingRequires requires) {
    var b =
        Binding.builder()
            .name(name)
            .capability(Capability.of("cap-" + name, ".", "."))
            .on(new ContextChangeTrigger(".go"))
            .requires(requires)
            .build();
    return b;
  }

  private Binding binding(String name) {
    return Binding.builder()
        .name(name)
        .capability(Capability.of("cap-" + name, ".", "."))
        .on(new ContextChangeTrigger(".go"))
        .build();
  }

  @Test
  void bindingWithoutRequiresPassesThrough() {
    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    var result = PlanningStrategyLoopControl.filterByRequires(List.of(binding("a")), plan);
    assertThat(result).hasSize(1);
  }

  @Test
  void bindingWithUnsatisfiedRequiresIsFiltered() {
    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    var agg =
        binding("aggregate", new BindingRequires(List.of("a", "b"), 2, OnThresholdReached.KEEP));
    var result = PlanningStrategyLoopControl.filterByRequires(List.of(agg), plan);
    assertThat(result).isEmpty();
  }

  @Test
  void bindingWithSatisfiedRequiresPassesThrough() {
    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    var piA = PlanItem.create("a", null, 0, null);
    piA.markRunning();
    piA.markCompleted();
    plan.addPlanItem(piA);

    var piB = PlanItem.create("b", null, 0, null);
    piB.markRunning();
    piB.markCompleted();
    plan.addPlanItem(piB);

    var agg =
        binding("aggregate", new BindingRequires(List.of("a", "b"), 2, OnThresholdReached.KEEP));
    var result = PlanningStrategyLoopControl.filterByRequires(List.of(agg), plan);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("aggregate");
  }

  @Test
  void partialCompletionNotEnough() {
    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    var piA = PlanItem.create("a", null, 0, null);
    piA.markRunning();
    piA.markCompleted();
    plan.addPlanItem(piA);

    var agg =
        binding(
            "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.KEEP));
    var result = PlanningStrategyLoopControl.filterByRequires(List.of(agg), plan);
    assertThat(result).isEmpty();
  }
}
