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

import java.util.Map;
import org.junit.jupiter.api.Test;

class CoordinationLifecycleEventTest {

  private static final TopologyDescriptor TOPOLOGY =
      new TopologyDescriptor(TopologyType.CENTRALISED, Map.of());

  @Test
  void sealed_interface_permits_three_variants() {
    assertThat(CoordinationLifecycleEvent.class.getPermittedSubclasses())
        .hasSize(3)
        .extracting(Class::getSimpleName)
        .containsExactlyInAnyOrder("Started", "Stopped", "Reconfigured");
  }

  @Test
  void started_valid_construction() {
    var event = new CoordinationLifecycleEvent.Started("coord-1", TOPOLOGY);
    assertThat(event.coordinatorId()).isEqualTo("coord-1");
    assertThat(event.topology()).isEqualTo(TOPOLOGY);
  }

  @Test
  void started_null_coordinatorId_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationLifecycleEvent.Started(null, TOPOLOGY));
  }

  @Test
  void started_null_topology_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationLifecycleEvent.Started("coord-1", null));
  }

  @Test
  void stopped_valid_construction() {
    var event = new CoordinationLifecycleEvent.Stopped("coord-1", "case completed");
    assertThat(event.coordinatorId()).isEqualTo("coord-1");
    assertThat(event.reason()).isEqualTo("case completed");
  }

  @Test
  void stopped_null_reason_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationLifecycleEvent.Stopped("coord-1", null));
  }

  @Test
  void reconfigured_valid_construction() {
    var newTopology = new TopologyDescriptor(TopologyType.DISTRIBUTED, Map.of());
    var event = new CoordinationLifecycleEvent.Reconfigured("coord-1", TOPOLOGY, newTopology);
    assertThat(event.previousTopology()).isEqualTo(TOPOLOGY);
    assertThat(event.newTopology()).isEqualTo(newTopology);
  }

  @Test
  void reconfigured_null_previous_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationLifecycleEvent.Reconfigured("coord-1", null, TOPOLOGY));
  }

  @Test
  void reconfigured_null_new_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationLifecycleEvent.Reconfigured("coord-1", TOPOLOGY, null));
  }
}
