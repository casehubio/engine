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
package io.casehub.engine.rest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.casehub.api.model.Binding;
import io.casehub.api.model.BindingRequires;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.api.view.CoordinationPipelineView;
import io.casehub.api.view.PhaseItemView;
import io.casehub.worker.api.Capability;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoordinationPipelineQuorumTest {

  private final CoordinationPipelineProjection projection = new CoordinationPipelineProjection();

  @Test
  void requiresBinding_populatesRequiredAndOf() {
    var def =
        CaseDefinition.builder()
            .namespace("ns")
            .name("quorum-test")
            .version("1.0")
            .capability(Capability.of("extract", ".", "."))
            .capability(Capability.of("aggregate", ".", "."))
            .binding(
                Binding.builder()
                    .name("extract-a")
                    .capability(Capability.of("extract", ".", "."))
                    .on("$.go")
                    .build())
            .binding(
                Binding.builder()
                    .name("extract-b")
                    .capability(Capability.of("extract", ".", "."))
                    .on("$.go")
                    .build())
            .binding(
                Binding.builder()
                    .name("extract-c")
                    .capability(Capability.of("extract", ".", "."))
                    .on("$.go")
                    .build())
            .binding(
                Binding.builder()
                    .name("aggregate")
                    .capability(Capability.of("aggregate", ".", "."))
                    .on("$.go")
                    .requires(
                        new BindingRequires(
                            List.of("extract-a", "extract-b", "extract-c"),
                            2,
                            OnThresholdReached.KEEP))
                    .build())
            .build();

    CoordinationPipelineView pipeline = projection.project(def);

    PhaseItemView aggPhase =
        pipeline.phases().stream()
            .filter(p -> "aggregate".equals(p.target()))
            .findFirst()
            .orElseThrow();

    assertEquals(2, aggPhase.required());
    assertEquals(3, aggPhase.of());
  }

  @Test
  void bindingWithoutRequires_hasNullRequiredAndOf() {
    var def =
        CaseDefinition.builder()
            .namespace("ns")
            .name("plain-test")
            .version("1.0")
            .capability(Capability.of("do-work", ".", "."))
            .binding(
                Binding.builder()
                    .name("work")
                    .capability(Capability.of("do-work", ".", "."))
                    .on("$.go")
                    .build())
            .build();

    CoordinationPipelineView pipeline = projection.project(def);

    PhaseItemView step = pipeline.phases().get(0);
    assertNull(step.required());
    assertNull(step.of());
  }
}
