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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CoordinationPatternTest {

  @Test
  void fourPatterns() {
    assertEquals(4, CoordinationPattern.values().length);
    assertNotNull(CoordinationPattern.SWARM);
    assertNotNull(CoordinationPattern.HIVE);
    assertNotNull(CoordinationPattern.MESH);
    assertNotNull(CoordinationPattern.MANY);
  }

  @Test
  void defaultAuthorityModels() {
    assertEquals(
        Optional.of(TopologyType.DISTRIBUTED), CoordinationPattern.SWARM.defaultAuthorityModel());
    assertEquals(
        Optional.of(TopologyType.CENTRALISED), CoordinationPattern.HIVE.defaultAuthorityModel());
    assertEquals(
        Optional.of(TopologyType.DISTRIBUTED), CoordinationPattern.MESH.defaultAuthorityModel());
    assertEquals(Optional.empty(), CoordinationPattern.MANY.defaultAuthorityModel());
  }

  @Test
  void fromTopologyString() {
    assertEquals(
        Optional.of(CoordinationPattern.SWARM), CoordinationPattern.fromTopologyString("swarm"));
    assertEquals(
        Optional.of(CoordinationPattern.HIVE), CoordinationPattern.fromTopologyString("hive"));
    assertEquals(
        Optional.of(CoordinationPattern.MESH), CoordinationPattern.fromTopologyString("mesh"));
    assertEquals(
        Optional.of(CoordinationPattern.MANY), CoordinationPattern.fromTopologyString("many"));
    assertEquals(
        Optional.of(CoordinationPattern.SWARM), CoordinationPattern.fromTopologyString("SWARM"));
    assertEquals(Optional.empty(), CoordinationPattern.fromTopologyString("custom-topology"));
    assertEquals(Optional.empty(), CoordinationPattern.fromTopologyString(null));
  }

  @Test
  void topologyDescriptorWithPattern() {
    var descriptor =
        new TopologyDescriptor(
            TopologyType.CENTRALISED,
            CoordinationPattern.HIVE,
            "group-1",
            Map.of("agent-1", AgentRole.COORDINATOR));
    assertEquals(CoordinationPattern.HIVE, descriptor.coordinationPattern());
    assertEquals("group-1", descriptor.groupId());
    assertEquals(TopologyType.CENTRALISED, descriptor.topologyType());
  }

  @Test
  void topologyDescriptorBackwardCompatible() {
    var descriptor = new TopologyDescriptor(TopologyType.DISTRIBUTED, Map.of());
    assertNull(descriptor.coordinationPattern());
    assertNull(descriptor.groupId());
    assertEquals(TopologyType.DISTRIBUTED, descriptor.topologyType());
  }
}
