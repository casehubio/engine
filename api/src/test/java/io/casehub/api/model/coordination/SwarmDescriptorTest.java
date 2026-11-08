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
package io.casehub.api.model.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SwarmDescriptorTest {

  @Test
  void creates_descriptor_with_all_fields() {
    var swarmId = UUID.randomUUID();
    var caseId = UUID.randomUUID();
    var descriptor =
        new SwarmDescriptor(
            swarmId, caseId, TopologyType.CENTRALISED, Set.of("analysis"), Set.of("agent-a"), null);
    assertThat(descriptor.swarmId()).isEqualTo(swarmId);
    assertThat(descriptor.caseId()).isEqualTo(caseId);
    assertThat(descriptor.topologyType()).isEqualTo(TopologyType.CENTRALISED);
    assertThat(descriptor.capabilities()).containsExactly("analysis");
    assertThat(descriptor.agentIds()).containsExactly("agent-a");
    assertThat(descriptor.parentSwarmId()).isNull();
  }

  @Test
  void creates_hierarchical_descriptor_with_parent() {
    var parentId = UUID.randomUUID();
    var descriptor =
        new SwarmDescriptor(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TopologyType.MULTI_SWARM,
            Set.of("remediation"),
            Set.of("agent-b"),
            parentId);
    assertThat(descriptor.parentSwarmId()).isEqualTo(parentId);
  }

  @Test
  void rejects_null_swarmId() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new SwarmDescriptor(
                    null, UUID.randomUUID(), TopologyType.CENTRALISED, Set.of(), Set.of(), null));
  }

  @Test
  void defensive_copies_sets() {
    var caps = new HashSet<>(Set.of("a", "b"));
    var descriptor =
        new SwarmDescriptor(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TopologyType.CENTRALISED,
            caps,
            Set.of("agent-a"),
            null);
    caps.add("c");
    assertThat(descriptor.capabilities()).hasSize(2);
  }
}
