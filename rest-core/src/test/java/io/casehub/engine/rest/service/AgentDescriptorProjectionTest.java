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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.CaseDefinition;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentQuery;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentDescriptorProjectionTest {

  @Test
  void returns_eidos_descriptor_when_registry_has_match() {
    var descriptor =
        AgentDescriptor.builder()
            .agentId("kyc-checker")
            .name("KYC Checker Agent")
            .description("Performs KYC verification")
            .slot("analyst")
            .tenancyId("t1")
            .capabilities(List.of(AgentCapability.builder().name("kyc").build()))
            .build();
    var registry = new StubAgentRegistry(Map.of("kyc-checker", descriptor));
    var projection = new AgentDescriptorProjection(registry);

    var caseDef =
        CaseDefinition.builder()
            .namespace("ns")
            .name("test")
            .version("1.0")
            .capability(Capability.of("kyc", ".", "."))
            .worker(
                Worker.builder()
                    .name("kyc-checker")
                    .capabilityName("kyc")
                    .function(WorkerFunction.NONE)
                    .description("KYC worker")
                    .build())
            .build();

    var result = projection.project(caseDef, "t1");

    assertEquals(1, result.size());
    assertEquals("KYC Checker Agent", result.getFirst().name());
    assertEquals("analyst", result.getFirst().slot());
  }

  @Test
  void returns_thin_descriptor_when_registry_has_no_match() {
    var registry = new StubAgentRegistry(Map.of());
    var projection = new AgentDescriptorProjection(registry);

    var caseDef =
        CaseDefinition.builder()
            .namespace("ns")
            .name("test")
            .version("1.0")
            .capability(Capability.of("risk-assessment", ".", "."))
            .worker(
                Worker.builder()
                    .name("risk-scorer")
                    .capabilityName("risk-assessment")
                    .function(WorkerFunction.NONE)
                    .description("Risk scoring worker")
                    .build())
            .build();

    var result = projection.project(caseDef, "t1");

    assertEquals(1, result.size());
    var thin = result.getFirst();
    assertEquals("risk-scorer", thin.agentId());
    assertEquals("risk-scorer", thin.name());
    assertEquals("Risk scoring worker", thin.description());
    assertEquals("worker", thin.slot());
    assertEquals(1, thin.capabilities().size());
    assertEquals("risk-assessment", thin.capabilities().getFirst().name());
  }

  @Test
  void handles_empty_workers() {
    var registry = new StubAgentRegistry(Map.of());
    var projection = new AgentDescriptorProjection(registry);

    var caseDef = CaseDefinition.builder().namespace("ns").name("test").version("1.0").build();

    var result = projection.project(caseDef, "t1");

    assertTrue(result.isEmpty());
  }

  @Test
  void mixes_enriched_and_thin_descriptors() {
    var descriptor =
        AgentDescriptor.builder()
            .agentId("kyc-checker")
            .name("KYC Agent")
            .slot("analyst")
            .tenancyId("t1")
            .capabilities(List.of(AgentCapability.builder().name("kyc").build()))
            .build();
    var registry = new StubAgentRegistry(Map.of("kyc-checker", descriptor));
    var projection = new AgentDescriptorProjection(registry);

    var caseDef =
        CaseDefinition.builder()
            .namespace("ns")
            .name("test")
            .version("1.0")
            .capability(Capability.of("kyc", ".", "."))
            .capability(Capability.of("misc", ".", "."))
            .worker(
                Worker.builder()
                    .name("kyc-checker")
                    .capabilityName("kyc")
                    .function(WorkerFunction.NONE)
                    .description("KYC")
                    .build())
            .worker(
                Worker.builder()
                    .name("unknown-worker")
                    .capabilityName("misc")
                    .function(WorkerFunction.NONE)
                    .build())
            .build();

    var result = projection.project(caseDef, "t1");

    assertEquals(2, result.size());
    assertEquals("KYC Agent", result.get(0).name());
    assertEquals("unknown-worker", result.get(1).name());
    assertEquals("worker", result.get(1).slot());
  }

  private static class StubAgentRegistry implements AgentRegistry {
    private final Map<String, AgentDescriptor> agents;

    StubAgentRegistry(Map<String, AgentDescriptor> agents) {
      this.agents = new HashMap<>(agents);
    }

    @Override
    public void register(AgentDescriptor descriptor) {
      agents.put(descriptor.agentId(), descriptor);
    }

    @Override
    public Optional<AgentDescriptor> findById(String agentId, String tenancyId) {
      return Optional.ofNullable(agents.get(agentId));
    }

    @Override
    public List<AgentMatch> find(AgentQuery query) {
      return List.of();
    }
  }
}
