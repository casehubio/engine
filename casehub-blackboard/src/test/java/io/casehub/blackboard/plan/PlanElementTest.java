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
package io.casehub.blackboard.plan;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.api.plan.PlanElement;
import org.junit.jupiter.api.Test;

class PlanElementTest {

  @Test
  void workerImplementsPlanElement() {
    Worker worker =
        Worker.builder()
            .name("w")
            .capabilities(
                Capability.builder().name("c").inputSchema("{}").outputSchema("{}").build())
            .function(input -> input)
            .build();
    assertThat(worker).isInstanceOf(PlanElement.class);
  }

  @Test
  void planElementIsAnInterface() {
    assertThat(PlanElement.class).isInterface();
  }
}
