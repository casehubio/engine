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

import io.casehub.api.model.CaseDefinition;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.worker.api.Worker;
import java.util.ArrayList;
import java.util.List;

public class AgentDescriptorProjection {

  private final AgentRegistry agentRegistry;

  public AgentDescriptorProjection(AgentRegistry agentRegistry) {
    this.agentRegistry = agentRegistry;
  }

  public List<AgentDescriptor> project(CaseDefinition def, String tenancyId) {
    List<Worker> workers = def.getWorkers() != null ? def.getWorkers() : List.of();
    List<AgentDescriptor> result = new ArrayList<>(workers.size());
    for (Worker w : workers) {
      agentRegistry
          .findById(w.name(), tenancyId)
          .ifPresentOrElse(result::add, () -> result.add(thinDescriptor(w, tenancyId)));
    }
    return result;
  }

  private AgentDescriptor thinDescriptor(Worker w, String tenancyId) {
    return AgentDescriptor.builder()
        .agentId(w.name())
        .name(w.name())
        .description(w.description())
        .slot("worker")
        .tenancyId(tenancyId)
        .capabilities(
            w.capabilities().stream()
                .map(cap -> AgentCapability.builder().name(cap).build())
                .toList())
        .build();
  }
}
