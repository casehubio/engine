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

import io.casehub.api.model.coordination.SwarmDescriptor;
import io.casehub.api.model.coordination.TopologyType;
import io.casehub.api.spi.coordination.RegistryListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryMultiSwarmRegistryTest {

  InMemoryMultiSwarmRegistry registry;
  UUID caseId;

  @BeforeEach
  void setUp() {
    registry = new InMemoryMultiSwarmRegistry();
    caseId = UUID.randomUUID();
  }

  @Test
  void register_and_query() {
    var desc =
        new SwarmDescriptor(
            UUID.randomUUID(),
            caseId,
            TopologyType.CENTRALISED,
            Set.of("analysis"),
            Set.of("a1"),
            null);
    registry.register(desc);
    assertThat(registry.activeSwarms(caseId)).hasSize(1);
    assertThat(registry.get(desc.swarmId())).isEqualTo(desc);
  }

  @Test
  void deregister_removes() {
    var desc =
        new SwarmDescriptor(
            UUID.randomUUID(),
            caseId,
            TopologyType.CENTRALISED,
            Set.of("analysis"),
            Set.of("a1"),
            null);
    registry.register(desc);
    registry.deregister(desc.swarmId());
    assertThat(registry.activeSwarms(caseId)).isEmpty();
    assertThat(registry.get(desc.swarmId())).isNull();
  }

  @Test
  void updateInterests_changes_capabilities() {
    var swarmId = UUID.randomUUID();
    registry.register(
        new SwarmDescriptor(
            swarmId, caseId, TopologyType.CENTRALISED, Set.of("analysis"), Set.of("a1"), null));
    registry.updateInterests(swarmId, Set.of("analysis", "remediation"));
    assertThat(registry.get(swarmId).capabilities())
        .containsExactlyInAnyOrder("analysis", "remediation");
  }

  @Test
  void children_returns_matching_parent() {
    var parentId = UUID.randomUUID();
    var childId = UUID.randomUUID();
    registry.register(
        new SwarmDescriptor(parentId, caseId, TopologyType.MULTI_SWARM, Set.of(), Set.of(), null));
    registry.register(
        new SwarmDescriptor(
            childId, caseId, TopologyType.CENTRALISED, Set.of("analysis"), Set.of("a1"), parentId));
    assertThat(registry.children(parentId)).hasSize(1);
    assertThat(registry.children(parentId).get(0).swarmId()).isEqualTo(childId);
  }

  @Test
  void listener_notified_on_register_deregister_update() {
    var events = new ArrayList<String>();
    registry.addListener(
        new RegistryListener() {
          @Override
          public void onRegistered(SwarmDescriptor d) {
            events.add("reg:" + d.swarmId());
          }

          @Override
          public void onDeregistered(UUID id) {
            events.add("dereg:" + id);
          }

          @Override
          public void onInterestsUpdated(UUID id, Set<String> caps) {
            events.add("update:" + id);
          }
        });
    var swarmId = UUID.randomUUID();
    var desc =
        new SwarmDescriptor(
            swarmId, caseId, TopologyType.CENTRALISED, Set.of("a"), Set.of("a1"), null);
    registry.register(desc);
    registry.updateInterests(swarmId, Set.of("b"));
    registry.deregister(swarmId);
    assertThat(events).containsExactly("reg:" + swarmId, "update:" + swarmId, "dereg:" + swarmId);
  }

  @Test
  void allActive_returns_all_cases() {
    var caseId2 = UUID.randomUUID();
    registry.register(
        new SwarmDescriptor(
            UUID.randomUUID(), caseId, TopologyType.CENTRALISED, Set.of("a"), Set.of("a1"), null));
    registry.register(
        new SwarmDescriptor(
            UUID.randomUUID(), caseId2, TopologyType.CENTRALISED, Set.of("b"), Set.of("b1"), null));
    assertThat(registry.allActive()).hasSize(2);
  }

  @Test
  void updateInterests_on_nonexistent_swarm_does_not_fire_listener() {
    var events = new ArrayList<String>();
    registry.addListener(
        new RegistryListener() {
          @Override
          public void onRegistered(SwarmDescriptor d) {
            events.add("reg");
          }

          @Override
          public void onDeregistered(UUID id) {
            events.add("dereg");
          }

          @Override
          public void onInterestsUpdated(UUID id, Set<String> caps) {
            events.add("update");
          }
        });
    registry.updateInterests(UUID.randomUUID(), Set.of("x"));
    assertThat(events).isEmpty();
  }
}
