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
package io.casehub.engine.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.AgentWorkerFunction;
import io.casehub.yaml.jackson.YamlMappers;
import org.junit.jupiter.api.Test;

class AgentWorkerFunctionProviderTest {

  private final AgentWorkerFunctionProvider provider = new AgentWorkerFunctionProvider();
  private final ObjectMapper yaml = YamlMappers.create();

  @Test
  void handles_returnsTrueForAgentBlock() throws Exception {
    var node =
        yaml.readTree(
            """
        name: analyst
        agent:
          slot: sentiment-analyst
        """);
    assertThat(provider.handles(node)).isTrue();
  }

  @Test
  void handles_returnsFalseWithoutAgentBlock() throws Exception {
    var node =
        yaml.readTree(
            """
        name: local-worker
        do:
          - step: something
        """);
    assertThat(provider.handles(node)).isFalse();
  }

  @Test
  void handles_returnsFalseForAgentWithoutSlot() throws Exception {
    var node =
        yaml.readTree(
            """
        name: inline-agent
        agent:
          systemPrompt: "You are a helper"
        """);
    assertThat(provider.handles(node)).isFalse();
  }

  @Test
  void create_parsesSlotOnly() throws Exception {
    var node =
        yaml.readTree(
            """
        name: analyst
        agent:
          slot: sentiment-analyst
        """);
    var function = (AgentWorkerFunction) provider.create(node);
    assertThat(function.isDeferred()).isTrue();
    assertThat(function.slotConfig().slot()).isEqualTo("sentiment-analyst");
    assertThat(function.slotConfig().model()).isNull();
    assertThat(function.slotConfig().userMessageTemplate()).isNull();
    assertThat(function.slotConfig().inputProjection()).isNull();
    assertThat(function.slotConfig().outputProjection()).isNull();
  }

  @Test
  void create_parsesAllOverrides() throws Exception {
    var node =
        yaml.readTree(
            """
        name: analyst
        agent:
          slot: sentiment-analyst
          model: claude-sonnet-5-5
          userMessage: "Analyse: {{text}}"
          inputProjection: ".ticket"
          outputProjection: ".sentiment"
        """);
    var function = (AgentWorkerFunction) provider.create(node);
    assertThat(function.slotConfig().slot()).isEqualTo("sentiment-analyst");
    assertThat(function.slotConfig().model()).isEqualTo("claude-sonnet-5-5");
    assertThat(function.slotConfig().userMessageTemplate()).isEqualTo("Analyse: {{text}}");
    assertThat(function.slotConfig().inputProjection()).isEqualTo(".ticket");
    assertThat(function.slotConfig().outputProjection()).isEqualTo(".sentiment");
  }
}
