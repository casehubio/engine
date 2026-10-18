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

public class AgentConverter {

  public static io.casehub.api.model.ai.WorkerAgentInvoker toWorkerInvoker(
      com.fasterxml.jackson.databind.JsonNode agentNode,
      io.casehub.platform.agent.AgentProvider agentProvider) {
    if (agentNode == null || agentNode.isNull()) {
      return null;
    }

    String model = null;
    if (agentNode.has("modelName")) {
      model = agentNode.get("modelName").asText();
    } else if (agentNode.has("model")) {
      com.fasterxml.jackson.databind.JsonNode modelNode = agentNode.get("model");
      if (modelNode.isTextual()) {
        model = modelNode.asText();
      }
    }

    String systemPrompt =
        agentNode.has("systemPrompt") ? agentNode.get("systemPrompt").asText() : null;

    java.util.function.UnaryOperator<com.fasterxml.jackson.databind.JsonNode> inputTransformer =
        agentNode.has("inputProjection")
            ? new io.casehub.api.model.ai.JqTransformer(agentNode.get("inputProjection").asText())
                ::apply
            : java.util.function.UnaryOperator.identity();

    java.util.function.UnaryOperator<com.fasterxml.jackson.databind.JsonNode> outputTransformer =
        agentNode.has("outputProjection")
            ? new io.casehub.api.model.ai.JqTransformer(agentNode.get("outputProjection").asText())
                ::apply
            : java.util.function.UnaryOperator.identity();

    String userMessageTemplate =
        agentNode.has("userMessageTemplate") ? agentNode.get("userMessageTemplate").asText() : null;

    return new io.casehub.api.model.ai.WorkerAgentInvoker(
        agentProvider,
        systemPrompt,
        userMessageTemplate,
        inputTransformer,
        outputTransformer,
        null,
        null,
        model,
        null);
  }
}
