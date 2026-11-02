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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TopologyDescriptor(TopologyType topologyType, Map<String, AgentRole> agentRoles) {

  public TopologyDescriptor {
    Objects.requireNonNull(topologyType, "topologyType must not be null");
    agentRoles = agentRoles != null ? Map.copyOf(agentRoles) : Map.of();
    for (var entry : agentRoles.entrySet()) {
      Objects.requireNonNull(entry.getKey(), "agent ID must not be null");
      Objects.requireNonNull(entry.getValue(), "agent role must not be null");
      AgentRole role = entry.getValue();
      if (role.structuralRole() != null) {
        topologyType.deriveAuthority(role.structuralRole());
      }
    }
  }

  public DecisionAuthority authorityFor(String agentId) {
    AgentRole role = agentRoles.get(agentId);
    if (role == null) {
      throw new IllegalArgumentException(
          "Unknown agent: " + agentId + " — not present in topology descriptor");
    }
    if (role.structuralRole() == null) {
      throw new IllegalArgumentException(
          "Agent "
              + agentId
              + " has symbolic role '"
              + role.name()
              + "' — symbolic roles have no structural position and cannot derive authority");
    }
    return topologyType.deriveAuthority(role.structuralRole());
  }

  public Optional<String> coordinatorAgentId() {
    return agentRoles.entrySet().stream()
        .filter(e -> e.getValue().structuralRole() == StructuralRole.INITIATOR)
        .map(Map.Entry::getKey)
        .findFirst();
  }
}
