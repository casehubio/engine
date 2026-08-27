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

import io.casehub.api.model.ai.AgentBuilder;
import io.casehub.api.model.ai.ChatModelProvider;
import io.casehub.api.model.ai.anthropic.AnthropicChatModelProvider;
import io.casehub.api.model.ai.gemini.GoogleAiGeminiChatModelProvider;
import io.casehub.api.model.ai.mistral.MistralAiChatModelProvider;
import io.casehub.api.model.ai.ollama.OllamaChatModelProvider;
import io.casehub.api.model.ai.openai.OpenAiChatModelProvider;

public class AgentConverter {

  /**
   * Builds an Agent directly from a raw YAML {@link com.fasterxml.jackson.databind.JsonNode},
   * bypassing the generated schema POJOs. Supports the flat YAML format where {@code model:} is the
   * provider name string and other fields ({@code modelName}, {@code apiKey}, etc.) sit at the same
   * level.
   */
  public static io.casehub.api.model.ai.Agent toApiAgent(
      com.fasterxml.jackson.databind.JsonNode agentNode) {
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
    ChatModelProvider modelProvider = toChatModelProviderFromNode(providerConfigNode, providerType);

    AgentBuilder builder =
        io.casehub.api.model.ai.Agent.builder()
            .systemPrompt(
                agentNode.has("systemPrompt") ? agentNode.get("systemPrompt").asText() : null)
            .model(modelProvider);

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

  private static ChatModelProvider toChatModelProviderFromNode(
      com.fasterxml.jackson.databind.JsonNode node, String providerType) {
    if (providerType == null) {
      throw new IllegalArgumentException("agent 'model' field (provider type) is required");
    }
    String modelName = node.has("modelName") ? node.get("modelName").asText() : null;
    String apiKey = node.has("apiKey") ? node.get("apiKey").asText() : null;
    Double temperature = node.has("temperature") ? node.get("temperature").asDouble() : null;
    Double topP = node.has("topP") ? node.get("topP").asDouble() : null;
    Integer maxTokens = node.has("maxTokens") ? node.get("maxTokens").asInt() : null;
    String baseUrl = node.has("baseUrl") ? node.get("baseUrl").asText() : null;

    return switch (providerType.toLowerCase()) {
      case "openai" -> {
        var b = OpenAiChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) {
          b.baseUrl(baseUrl);
        }
        if (temperature != null) {
          b.temperature(temperature);
        }
        if (topP != null) {
          b.topP(topP);
        }
        if (maxTokens != null) {
          b.maxTokens(maxTokens);
        }
        if (node.has("organizationId")) {
          b.organizationId(node.get("organizationId").asText());
        }
        yield b.build();
      }
      case "anthropic" -> {
        var b = AnthropicChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) {
          b.baseUrl(baseUrl);
        }
        if (temperature != null) {
          b.temperature(temperature);
        }
        if (topP != null) {
          b.topP(topP);
        }
        if (maxTokens != null) {
          b.maxTokens(maxTokens);
        }
        if (node.has("version")) {
          b.version(node.get("version").asText());
        }
        if (node.has("topK")) {
          b.topK(node.get("topK").asInt());
        }
        yield b.build();
      }
      case "ollama" -> {
        var b = OllamaChatModelProvider.builder().baseUrl(baseUrl).modelName(modelName);
        if (temperature != null) {
          b.temperature(temperature);
        }
        if (topP != null) {
          b.topP(topP);
        }
        yield b.build();
      }
      case "mistralai", "mistral" -> {
        var b = MistralAiChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) {
          b.baseUrl(baseUrl);
        }
        if (temperature != null) {
          b.temperature(temperature);
        }
        if (topP != null) {
          b.topP(topP);
        }
        if (maxTokens != null) {
          b.maxTokens(maxTokens);
        }
        yield b.build();
      }
      case "googleaigemini", "gemini" -> {
        var b = GoogleAiGeminiChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (temperature != null) {
          b.temperature(temperature);
        }
        if (topP != null) {
          b.topP(topP);
        }
        if (maxTokens != null) {
          b.maxOutputTokens(maxTokens);
        }
        yield b.build();
      }
      default -> throw new IllegalArgumentException("Unknown model provider: " + providerType);
    };
  }
}
