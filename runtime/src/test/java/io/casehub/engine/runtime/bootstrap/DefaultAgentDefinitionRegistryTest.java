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
package io.casehub.engine.runtime.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.common.spi.AgentDefinition;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultAgentDefinitionRegistryTest {

  private DefaultAgentDefinitionRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new DefaultAgentDefinitionRegistry();
  }

  @Test
  void registerAndFindByName() {
    var agent = new AgentDefinition("planner", "llm", "Plans tasks", Map.of("model", "gpt-4"));
    registry.registerAgent(agent);

    assertThat(registry.findByName("planner"))
        .hasValueSatisfying(
            found -> {
              assertThat(found.name()).isEqualTo("planner");
              assertThat(found.type()).isEqualTo("llm");
              assertThat(found.description()).isEqualTo("Plans tasks");
              assertThat(found.configuration()).containsEntry("model", "gpt-4");
            });
  }

  @Test
  void findByName_unknownReturnsEmpty() {
    assertThat(registry.findByName("nonexistent")).isEmpty();
  }

  @Test
  void duplicateRegistration_keepsOriginal() {
    var original = new AgentDefinition("router", "rule", "Routes work", Map.of());
    var duplicate = new AgentDefinition("router", "llm", "Different", Map.of("key", "val"));

    registry.registerAgent(original);
    registry.registerAgent(duplicate);

    assertThat(registry.findByName("router"))
        .hasValueSatisfying(found -> assertThat(found.type()).isEqualTo("rule"));
  }

  @Test
  void allAgents_returnsAllRegistered() {
    registry.registerAgent(new AgentDefinition("a1", "t1", "d1", Map.of()));
    registry.registerAgent(new AgentDefinition("a2", "t2", "d2", Map.of()));
    registry.registerAgent(new AgentDefinition("a3", "t3", "d3", Map.of()));

    assertThat(registry.allAgents()).hasSize(3);
    assertThat(registry.allAgents())
        .extracting(AgentDefinition::name)
        .containsExactlyInAnyOrder("a1", "a2", "a3");
  }

  @Test
  void allAgents_emptyWhenNoneRegistered() {
    assertThat(registry.allAgents()).isEmpty();
  }

  @Test
  void nullConfiguration_defaultsToEmptyMap() {
    var agent = new AgentDefinition("bare", "type", "desc", null);
    registry.registerAgent(agent);

    assertThat(registry.findByName("bare"))
        .hasValueSatisfying(found -> assertThat(found.configuration()).isEmpty());
  }
}
