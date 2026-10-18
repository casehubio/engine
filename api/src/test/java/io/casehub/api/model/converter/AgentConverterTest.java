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
package io.casehub.api.model.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

class AgentConverterTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final AgentProvider MOCK_PROVIDER =
      new AgentProvider() {
        @Override
        public Multi<AgentEvent> invoke(AgentSessionConfig c) {
          return Multi.createFrom().empty();
        }

        @Override
        public AgentSession openSession(AgentSessionInit i) {
          throw new UnsupportedOperationException();
        }
      };

  @Test
  void toWorkerInvoker_nullInput_returnsNull() {
    var result = AgentConverter.toWorkerInvoker(null, MOCK_PROVIDER);
    assertThat(result).isNull();
  }

  @Test
  void toWorkerInvoker_nullJsonNode_returnsNull() throws Exception {
    var result = AgentConverter.toWorkerInvoker(JSON.readTree("null"), MOCK_PROVIDER);
    assertThat(result).isNull();
  }

  @Test
  void toWorkerInvoker_buildsFromYamlNode() {
    var agentNode = JSON.createObjectNode();
    agentNode.put("systemPrompt", "You are helpful");
    agentNode.put("model", "anthropic");
    agentNode.put("modelName", "claude-3-5-sonnet");
    agentNode.put("inputProjection", ".data");
    agentNode.put("outputProjection", ".result");
    agentNode.put("userMessageTemplate", "Process: {{input}}");

    var invoker = AgentConverter.toWorkerInvoker(agentNode, MOCK_PROVIDER);
    assertThat(invoker).isNotNull();
    assertThat(invoker.model()).isEqualTo("claude-3-5-sonnet");
  }

  @Test
  void toWorkerInvoker_modelNameTakesPrecedence() {
    var agentNode = JSON.createObjectNode();
    agentNode.put("systemPrompt", "test");
    agentNode.put("model", "anthropic");
    agentNode.put("modelName", "claude-3-haiku");

    var invoker = AgentConverter.toWorkerInvoker(agentNode, MOCK_PROVIDER);
    assertThat(invoker).isNotNull();
    assertThat(invoker.model()).isEqualTo("claude-3-haiku");
  }

  @Test
  void toWorkerInvoker_fallsBackToModelField() {
    var agentNode = JSON.createObjectNode();
    agentNode.put("systemPrompt", "test");
    agentNode.put("model", "gpt-4o-mini");

    var invoker = AgentConverter.toWorkerInvoker(agentNode, MOCK_PROVIDER);
    assertThat(invoker).isNotNull();
    assertThat(invoker.model()).isEqualTo("gpt-4o-mini");
  }
}
