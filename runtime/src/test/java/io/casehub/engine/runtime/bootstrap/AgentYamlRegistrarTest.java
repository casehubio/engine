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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.casehub.engine.common.spi.AgentDefinition;
import io.casehub.engine.common.spi.AgentDefinitionRegistry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentYamlRegistrarTest {

  @Mock AgentDefinitionRegistry agentDefinitionRegistry;
  @InjectMocks AgentYamlRegistrar registrar;

  @Test
  void sectionKey() {
    assertThat(registrar.sectionKey()).isEqualTo("agents");
  }

  @Test
  void classpathPaths() {
    assertThat(registrar.classpathPaths()).isEqualTo(Set.of("agents"));
  }

  @Test
  void priority() {
    assertThat(registrar.priority()).isEqualTo(100);
  }

  @Test
  void register_validYaml_registersAgent() {
    String yaml =
        """
        name: planner
        type: llm
        description: Plans work decomposition
        configuration:
          model: gpt-4
          temperature: 0.7
        """;

    registrar.register("agents/planner.yaml", stream(yaml));

    var captor = ArgumentCaptor.forClass(AgentDefinition.class);
    verify(agentDefinitionRegistry).registerAgent(captor.capture());

    AgentDefinition agent = captor.getValue();
    assertThat(agent.name()).isEqualTo("planner");
    assertThat(agent.type()).isEqualTo("llm");
    assertThat(agent.description()).isEqualTo("Plans work decomposition");
    assertThat(agent.configuration())
        .containsEntry("model", "gpt-4")
        .containsEntry("temperature", 0.7);
  }

  @Test
  void register_minimalYaml_registersWithNulls() {
    String yaml = "name: simple\n";

    registrar.register("agents/simple.yaml", stream(yaml));

    verify(agentDefinitionRegistry)
        .registerAgent(argThat(a -> "simple".equals(a.name()) && a.type() == null));
  }

  @Test
  void register_emptyYaml_doesNotRegister() {
    registrar.register("agents/empty.yaml", stream(""));

    verify(agentDefinitionRegistry, never()).registerAgent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void register_missingName_doesNotRegister() {
    String yaml =
        """
        type: llm
        description: No name field
        """;

    registrar.register("agents/noname.yaml", stream(yaml));

    verify(agentDefinitionRegistry, never()).registerAgent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void register_noConfiguration_defaultsToEmptyMap() {
    String yaml =
        """
        name: bare-agent
        type: rule
        """;

    registrar.register("agents/bare.yaml", stream(yaml));

    verify(agentDefinitionRegistry).registerAgent(argThat(a -> a.configuration().isEmpty()));
  }

  private static ByteArrayInputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }
}
