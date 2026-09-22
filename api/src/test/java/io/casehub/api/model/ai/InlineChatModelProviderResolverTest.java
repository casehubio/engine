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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InlineChatModelProviderResolverTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private final ChatModelProviderResolver resolver = InlineChatModelProviderResolver.INSTANCE;

  @Test
  void resolve_nullProviderType_throws() {
    assertThatThrownBy(() -> resolver.resolve(null, JSON.readTree("{}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("provider type");
  }

  @Test
  void resolve_unknownProvider_throws() throws Exception {
    assertThatThrownBy(() -> resolver.resolve("unknown-llm", JSON.readTree("{}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown model provider");
  }

  @Test
  void resolve_openai_allFields() throws Exception {
    JsonNode config =
        JSON.readTree(
            """
        {"modelName":"gpt-4","apiKey":"sk-test",
         "baseUrl":"http://localhost:3000/v1","organizationId":"org-test",
         "temperature":0.7,"topP":0.9,"maxTokens":1024}""");
    ChatModelProvider result = resolver.resolve("openai", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.OPENAI);
  }

  @Test
  void resolve_anthropic_allFields() throws Exception {
    JsonNode config =
        JSON.readTree(
            """
        {"modelName":"claude-3-sonnet-20240229","apiKey":"sk-ant-test",
         "baseUrl":"https://custom.example.com","version":"2023-06-01",
         "temperature":0.3,"topP":0.95,"topK":40,"maxTokens":2048}""");
    ChatModelProvider result = resolver.resolve("anthropic", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.ANTHROPIC);
  }

  @Test
  void resolve_ollama() throws Exception {
    JsonNode config =
        JSON.readTree(
            "{\"baseUrl\":\"http://localhost:11434\",\"modelName\":\"llama2\",\"temperature\":0.5}");
    ChatModelProvider result = resolver.resolve("ollama", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.OLLAMA);
  }

  @Test
  void resolve_mistral() throws Exception {
    JsonNode config =
        JSON.readTree("{\"modelName\":\"mistral-large-latest\",\"apiKey\":\"msk-test\"}");
    ChatModelProvider result = resolver.resolve("mistralai", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.MISTRAL);
  }

  @Test
  void resolve_mistral_alias() throws Exception {
    JsonNode config = JSON.readTree("{\"modelName\":\"mistral-small\",\"apiKey\":\"msk-key\"}");
    ChatModelProvider result = resolver.resolve("mistral", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.MISTRAL);
  }

  @Test
  void resolve_gemini() throws Exception {
    JsonNode config =
        JSON.readTree(
            "{\"modelName\":\"gemini-pro\",\"apiKey\":\"gai-test\",\"temperature\":0.6,\"maxTokens\":1500}");
    ChatModelProvider result = resolver.resolve("googleaigemini", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.GOOGLE_AI_GEMINI);
  }

  @Test
  void resolve_gemini_alias() throws Exception {
    JsonNode config = JSON.readTree("{\"modelName\":\"gemini-1.5-pro\",\"apiKey\":\"gai-key\"}");
    ChatModelProvider result = resolver.resolve("gemini", config);
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(ModelType.GOOGLE_AI_GEMINI);
  }
}
