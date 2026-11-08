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

import io.casehub.api.model.coordination.SwarmDescriptor;
import io.casehub.api.spi.coordination.MultiSwarmRegistry;
import io.casehub.api.spi.coordination.RegistryListener;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;

public class InMemoryMultiSwarmRegistry implements MultiSwarmRegistry {

  private final ConcurrentHashMap<UUID, SwarmDescriptor> swarms = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<RegistryListener> listeners = new CopyOnWriteArrayList<>();

  @Override
  public void register(SwarmDescriptor descriptor) {
    Objects.requireNonNull(descriptor);
    swarms.put(descriptor.swarmId(), descriptor);
    listeners.forEach(l -> l.onRegistered(descriptor));
  }

  @Override
  public void deregister(UUID swarmId) {
    if (swarms.remove(swarmId) != null) {
      listeners.forEach(l -> l.onDeregistered(swarmId));
    }
  }

  @Override
  public void updateInterests(UUID swarmId, Set<String> capabilities) {
    swarms.computeIfPresent(
        swarmId,
        (id, old) ->
            new SwarmDescriptor(
                old.swarmId(),
                old.caseId(),
                old.topologyType(),
                capabilities,
                old.agentIds(),
                old.parentSwarmId()));
    listeners.forEach(l -> l.onInterestsUpdated(swarmId, capabilities));
  }

  @Override
  public List<SwarmDescriptor> activeSwarms(UUID caseId) {
    return swarms.values().stream().filter(d -> d.caseId().equals(caseId)).toList();
  }

  @Override
  public List<SwarmDescriptor> allActive() {
    return List.copyOf(swarms.values());
  }

  @Override
  public List<SwarmDescriptor> children(UUID parentSwarmId) {
    return swarms.values().stream().filter(d -> parentSwarmId.equals(d.parentSwarmId())).toList();
  }

  @Override
  public @Nullable SwarmDescriptor get(UUID swarmId) {
    return swarms.get(swarmId);
  }

  @Override
  public void addListener(RegistryListener listener) {
    listeners.add(Objects.requireNonNull(listener));
  }
}
