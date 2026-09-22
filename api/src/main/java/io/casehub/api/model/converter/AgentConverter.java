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

  /**
   * Builds an Agent directly from a raw YAML {@link com.fasterxml.jackson.databind.JsonNode},
   * bypassing the generated schema POJOs. Supports the flat YAML format where {@code model:} is the
   * provider name string and other fields ({@code modelName}, {@code apiKey}, etc.) sit at the same
   * level.
   */
  public static io.casehub.api.model.ai.Agent toApiAgent(
      com.fasterxml.jackson.databind.JsonNode agentNode,
      io.casehub.api.model.ai.ChatModelProviderResolver resolver) {
    if (agentNode == null || agentNode.isNull()) {
      return null;
    }

    String providerType;
    com.fasterxml.jackson.databind.JsonNode providerConfigNode;
    com.fasterxml.jackson.databind.JsonNode modelNode = agentNode.get("model");
    if (modelNode != null && modelNode.isObject() && modelNode.size() > 0) {
      var entry = modelNode.fields().next();
      providerType = entry.getKey();
      providerConfigNode = entry.getValue();
    } else {
      providerType = modelNode != null ? modelNode.asText() : null;
      providerConfigNode = agentNode;
    }
    io.casehub.api.model.ai.ChatModelProvider modelProvider =
        resolver.resolve(providerType, providerConfigNode);

    String modelNameForId =
        providerConfigNode.has("modelName") ? providerConfigNode.get("modelName").asText() : null;

    io.casehub.api.model.ai.AgentBuilder builder =
        io.casehub.api.model.ai.Agent.builder()
            .systemPrompt(
                agentNode.has("systemPrompt") ? agentNode.get("systemPrompt").asText() : null)
            .model(modelProvider)
            .modelId(modelNameForId);

    if (agentNode.has("inputProjection")) {
      builder.inputProjection(agentNode.get("inputProjection").asText());
    }
    if (agentNode.has("outputProjection")) {
      builder.outputProjection(agentNode.get("outputProjection").asText());
    }
    if (agentNode.has("userMessageTemplate")) {
      builder.userMessage(agentNode.get("userMessageTemplate").asText());
    }

    return builder.build();
  }
}
