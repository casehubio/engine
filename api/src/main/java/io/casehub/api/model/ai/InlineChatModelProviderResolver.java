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
package io.casehub.api.model.ai;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.model.ai.anthropic.AnthropicChatModelProvider;
import io.casehub.api.model.ai.gemini.GoogleAiGeminiChatModelProvider;
import io.casehub.api.model.ai.mistral.MistralAiChatModelProvider;
import io.casehub.api.model.ai.ollama.OllamaChatModelProvider;
import io.casehub.api.model.ai.openai.OpenAiChatModelProvider;

/**
 * Default {@link ChatModelProviderResolver} that constructs LangChain4j provider instances inline
 * from YAML configuration. This is the extraction of the original 5-way switch from {@code
 * AgentConverter.toChatModelProviderFromNode()}.
 */
public final class InlineChatModelProviderResolver implements ChatModelProviderResolver {

  public static final InlineChatModelProviderResolver INSTANCE =
      new InlineChatModelProviderResolver();

  private InlineChatModelProviderResolver() {}

  @Override
  public ChatModelProvider resolve(String providerType, JsonNode config) {
    if (providerType == null) {
      throw new IllegalArgumentException("agent 'model' field (provider type) is required");
    }
    String modelName = config.has("modelName") ? config.get("modelName").asText() : null;
    String apiKey = config.has("apiKey") ? config.get("apiKey").asText() : null;
    Double temperature = config.has("temperature") ? config.get("temperature").asDouble() : null;
    Double topP = config.has("topP") ? config.get("topP").asDouble() : null;
    Integer maxTokens = config.has("maxTokens") ? config.get("maxTokens").asInt() : null;
    String baseUrl = config.has("baseUrl") ? config.get("baseUrl").asText() : null;

    return switch (providerType.toLowerCase()) {
      case "openai" -> {
        var b = OpenAiChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) b.baseUrl(baseUrl);
        if (temperature != null) b.temperature(temperature);
        if (topP != null) b.topP(topP);
        if (maxTokens != null) b.maxTokens(maxTokens);
        if (config.has("organizationId")) b.organizationId(config.get("organizationId").asText());
        yield b.build();
      }
      case "anthropic" -> {
        var b = AnthropicChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) b.baseUrl(baseUrl);
        if (temperature != null) b.temperature(temperature);
        if (topP != null) b.topP(topP);
        if (maxTokens != null) b.maxTokens(maxTokens);
        if (config.has("version")) b.version(config.get("version").asText());
        if (config.has("topK")) b.topK(config.get("topK").asInt());
        yield b.build();
      }
      case "ollama" -> {
        var b = OllamaChatModelProvider.builder().baseUrl(baseUrl).modelName(modelName);
        if (temperature != null) b.temperature(temperature);
        if (topP != null) b.topP(topP);
        yield b.build();
      }
      case "mistralai", "mistral" -> {
        var b = MistralAiChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (baseUrl != null) b.baseUrl(baseUrl);
        if (temperature != null) b.temperature(temperature);
        if (topP != null) b.topP(topP);
        if (maxTokens != null) b.maxTokens(maxTokens);
        yield b.build();
      }
      case "googleaigemini", "gemini" -> {
        var b = GoogleAiGeminiChatModelProvider.builder().apiKey(apiKey).modelName(modelName);
        if (temperature != null) b.temperature(temperature);
        if (topP != null) b.topP(topP);
        if (maxTokens != null) b.maxOutputTokens(maxTokens);
        yield b.build();
      }
      default -> throw new IllegalArgumentException("Unknown model provider: " + providerType);
    };
  }
}
