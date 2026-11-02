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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TopologyDescriptorTest {

  @Test
  void constructs_with_valid_centralised_topology() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.CENTRALISED,
            Map.of(
                "coord",
                AgentRole.COORDINATOR,
                "worker-1",
                AgentRole.of("worker", StructuralRole.CONTRACTOR)));
    assertThat(descriptor.topologyType()).isEqualTo(TopologyType.CENTRALISED);
    assertThat(descriptor.agentRoles()).hasSize(2);
  }

  @Test
  void constructs_with_valid_distributed_topology() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.DISTRIBUTED,
            Map.of(
                "peer-1", AgentRole.of("analyst", StructuralRole.PEER),
                "peer-2", AgentRole.of("reviewer", StructuralRole.PEER)));
    assertThat(descriptor.topologyType()).isEqualTo(TopologyType.DISTRIBUTED);
    assertThat(descriptor.agentRoles()).hasSize(2);
  }

  @Test
  void constructs_with_empty_role_map() {
    var descriptor = new TopologyDescriptor(TopologyType.CENTRALISED, Map.of());
    assertThat(descriptor.agentRoles()).isEmpty();
  }

  @Test
  void constructs_with_null_role_map_defaults_to_empty() {
    var descriptor = new TopologyDescriptor(TopologyType.CENTRALISED, null);
    assertThat(descriptor.agentRoles()).isEmpty();
  }

  @Test
  void rejects_null_topology_type() {
    assertThatNullPointerException()
        .isThrownBy(() -> new TopologyDescriptor(null, Map.of()))
        .withMessageContaining("topologyType");
  }

  @Test
  void rejects_invalid_role_topology_combination() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new TopologyDescriptor(
                    TopologyType.CENTRALISED,
                    Map.of("agent-1", AgentRole.of("analyst", StructuralRole.PEER))));
  }

  @Test
  void allows_symbolic_roles_without_validation() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.CENTRALISED, Map.of("observer", AgentRole.of("observer")));
    assertThat(descriptor.agentRoles()).hasSize(1);
  }

  @Test
  void role_map_is_immutable() {
    var mutable = new HashMap<String, AgentRole>();
    mutable.put("coord", AgentRole.COORDINATOR);
    var descriptor = new TopologyDescriptor(TopologyType.CENTRALISED, mutable);
    mutable.put("intruder", AgentRole.of("intruder", StructuralRole.CONTRACTOR));
    assertThat(descriptor.agentRoles()).hasSize(1);
  }

  @Test
  void authority_for_known_agent_with_structural_role() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.CENTRALISED,
            Map.of(
                "coord",
                AgentRole.COORDINATOR,
                "worker-1",
                AgentRole.of("worker", StructuralRole.CONTRACTOR)));
    assertThat(descriptor.authorityFor("coord")).isEqualTo(DecisionAuthority.CENTRAL);
    assertThat(descriptor.authorityFor("worker-1")).isEqualTo(DecisionAuthority.LOCAL);
  }

  @Test
  void authority_for_distributed_peers() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.DISTRIBUTED,
            Map.of("peer-1", AgentRole.of("analyst", StructuralRole.PEER)));
    assertThat(descriptor.authorityFor("peer-1")).isEqualTo(DecisionAuthority.COORDINATED);
  }

  @Test
  void authority_for_unknown_agent_throws() {
    var descriptor = new TopologyDescriptor(TopologyType.CENTRALISED, Map.of());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> descriptor.authorityFor("unknown"))
        .withMessageContaining("unknown");
  }

  @Test
  void authority_for_symbolic_role_throws() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.CENTRALISED, Map.of("observer", AgentRole.of("observer")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> descriptor.authorityFor("observer"))
        .withMessageContaining("symbolic");
  }

  @Test
  void coordinator_agent_id_returns_initiator() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.CENTRALISED,
            Map.of(
                "coord",
                AgentRole.COORDINATOR,
                "worker-1",
                AgentRole.of("worker", StructuralRole.CONTRACTOR)));
    assertThat(descriptor.coordinatorAgentId()).contains("coord");
  }

  @Test
  void coordinator_agent_id_empty_for_distributed() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.DISTRIBUTED,
            Map.of("peer-1", AgentRole.of("analyst", StructuralRole.PEER)));
    assertThat(descriptor.coordinatorAgentId()).isEmpty();
  }

  @Test
  void coordinator_agent_id_empty_for_empty_map() {
    var descriptor = new TopologyDescriptor(TopologyType.CENTRALISED, Map.of());
    assertThat(descriptor.coordinatorAgentId()).isEmpty();
  }

  @Test
  void hybrid_topology_all_roles_valid() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.HYBRID,
            Map.of(
                "coord", AgentRole.COORDINATOR,
                "peer-1", AgentRole.of("analyst", StructuralRole.PEER),
                "worker-1", AgentRole.of("worker", StructuralRole.CONTRACTOR)));
    assertThat(descriptor.authorityFor("coord")).isEqualTo(DecisionAuthority.CENTRAL);
    assertThat(descriptor.authorityFor("peer-1")).isEqualTo(DecisionAuthority.COORDINATED);
    assertThat(descriptor.authorityFor("worker-1")).isEqualTo(DecisionAuthority.LOCAL);
  }
}
