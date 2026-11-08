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
package io.casehub.api.spi.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.coordination.SwarmDescriptor;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MultiSwarmRegistryTest {

  @Test
  void interface_methods_exist() {
    MultiSwarmRegistry registry =
        new MultiSwarmRegistry() {
          @Override
          public void register(SwarmDescriptor descriptor) {}

          @Override
          public void deregister(UUID swarmId) {}

          @Override
          public void updateInterests(UUID swarmId, Set<String> capabilities) {}

          @Override
          public List<SwarmDescriptor> activeSwarms(UUID caseId) {
            return List.of();
          }

          @Override
          public List<SwarmDescriptor> allActive() {
            return List.of();
          }

          @Override
          public List<SwarmDescriptor> children(UUID parentSwarmId) {
            return List.of();
          }

          @Override
          public SwarmDescriptor get(UUID swarmId) {
            return null;
          }

          @Override
          public void addListener(RegistryListener listener) {}
        };
    assertThat(registry).isNotNull();
  }

  @Test
  void listener_interface_methods_exist() {
    RegistryListener listener =
        new RegistryListener() {
          @Override
          public void onRegistered(SwarmDescriptor descriptor) {}

          @Override
          public void onDeregistered(UUID swarmId) {}

          @Override
          public void onInterestsUpdated(UUID swarmId, Set<String> capabilities) {}
        };
    assertThat(listener).isNotNull();
  }
}
