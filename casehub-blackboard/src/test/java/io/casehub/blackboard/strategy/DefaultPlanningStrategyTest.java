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
package io.casehub.blackboard.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.blackboard.plan.PlanItem;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultPlanningStrategyTest {

  private final DefaultPlanningStrategy strategy = new DefaultPlanningStrategy();

  private static Worker worker(String name) {
    return Worker.builder()
        .name(name)
        .capabilities(
            Capability.builder().name("cap-" + name).inputSchema("{}").outputSchema("{}").build())
        .function(input -> input)
        .build();
  }

  @Test
  void returnsAllEligibleItems() {
    PlanItem<Worker> w1 = PlanItem.of(worker("a"));
    PlanItem<Worker> w2 = PlanItem.of(worker("b"));
    List<PlanItem<?>> result = strategy.select(null, List.of(w1, w2));
    assertThat(result).containsExactly(w1, w2);
  }

  @Test
  void returnsEmptyForEmptyEligible() {
    List<PlanItem<?>> result = strategy.select(null, List.of());
    assertThat(result).isEmpty();
  }

  @Test
  void preservesOrder() {
    PlanItem<Worker> w1 = PlanItem.of(worker("a"));
    PlanItem<Worker> w2 = PlanItem.of(worker("b"));
    PlanItem<Worker> w3 = PlanItem.of(worker("c"));
    List<PlanItem<?>> result = strategy.select(null, List.of(w1, w2, w3));
    assertThat(result).containsExactly(w1, w2, w3);
  }
}
