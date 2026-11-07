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
package io.casehub.engine.runtime.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.coordination.TopologyType;
import org.junit.jupiter.api.Test;

class CentralisedTopologyProviderTest {

  private final CentralisedTopologyProvider provider = new CentralisedTopologyProvider();

  @Test
  void returns_centralised_topology() {
    var descriptor = provider.topologyFor("agent-1", "tenant-1");
    assertThat(descriptor.topologyType()).isEqualTo(TopologyType.CENTRALISED);
  }

  @Test
  void returns_empty_role_map() {
    var descriptor = provider.topologyFor("agent-1", "tenant-1");
    assertThat(descriptor.agentRoles()).isEmpty();
  }

  @Test
  void returns_same_shape_for_different_agents() {
    var d1 = provider.topologyFor("agent-1", "tenant-1");
    var d2 = provider.topologyFor("agent-2", "tenant-1");
    assertThat(d1.topologyType()).isEqualTo(d2.topologyType());
    assertThat(d1.agentRoles()).isEqualTo(d2.agentRoles());
  }
}
