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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.ai.Agent;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AgentConverter} — verifies null handling, error paths, and all 5 provider
 * dispatch branches. Pure unit tests, no Quarkus. See casehubio/engine#358.
 */
class AgentConverterTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  // ---- null / error handling ------------------------------------------------

  @Test
  void toApiAgent_nullInput_returnsNull() {
    Agent result = AgentConverter.toApiAgent((JsonNode) null);
    assertThat(result).isNull();
  }

  @Test
  void toApiAgent_nullJsonNode_returnsNull() throws Exception {
    JsonNode node = JSON.readTree("null");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNull();
  }

  @Test
  void toApiAgent_missingModel_throwsIllegalArgument() throws Exception {
    JsonNode node = JSON.readTree("{\"systemPrompt\": \"You are a test agent\"}");
    assertThatThrownBy(() -> AgentConverter.toApiAgent(node))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("model");
  }

  @Test
  void toApiAgent_unknownProvider_throwsIllegalArgument() throws Exception {
    JsonNode node = JSON.readTree("{\"model\": \"unknown-llm\", \"modelName\": \"x\"}");
    assertThatThrownBy(() -> AgentConverter.toApiAgent(node))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown model provider");
  }

  // ---- OpenAI provider ------------------------------------------------------

  @Test
  void toApiAgent_openai_allFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            """
        {"model":"openai","modelName":"gpt-4","apiKey":"sk-test-key",
         "baseUrl":"http://openclaw:3000/v1","organizationId":"org-test",
         "temperature":0.7,"topP":0.9,"maxTokens":1024,
         "systemPrompt":"You are a test agent"}""");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  @Test
  void toApiAgent_openai_minimalFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"openai\",\"modelName\":\"gpt-4o-mini\",\"apiKey\":\"sk-test-key\",\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  // ---- Ollama provider ------------------------------------------------------

  @Test
  void toApiAgent_ollama_allFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"ollama\",\"baseUrl\":\"http://localhost:11434\","
                + "\"modelName\":\"llama2\",\"temperature\":0.5,\"topP\":0.8,\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  @Test
  void toApiAgent_ollama_minimalFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"ollama\",\"baseUrl\":\"http://localhost:11434\","
                + "\"modelName\":\"mistral\",\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  // ---- Anthropic provider ---------------------------------------------------

  @Test
  void toApiAgent_anthropic_allFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            """
        {"model":"anthropic","modelName":"claude-3-sonnet-20240229",
         "apiKey":"sk-ant-test-key","baseUrl":"https://custom-anthropic.example.com",
         "version":"2023-06-01","temperature":0.3,"topP":0.95,"topK":40,"maxTokens":2048,
         "systemPrompt":"You are a test agent"}""");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  @Test
  void toApiAgent_anthropic_minimalFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"anthropic\",\"modelName\":\"claude-3-haiku-20240307\","
                + "\"apiKey\":\"sk-ant-key\",\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  // ---- Mistral provider -----------------------------------------------------

  @Test
  void toApiAgent_mistral_allFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"mistralai\",\"modelName\":\"mistral-large-latest\","
                + "\"apiKey\":\"msk-test-key\",\"baseUrl\":\"https://custom-mistral.example.com\","
                + "\"temperature\":0.4,\"topP\":0.85,\"maxTokens\":512,\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  @Test
  void toApiAgent_mistral_minimalFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"mistral\",\"modelName\":\"mistral-small\",\"apiKey\":\"msk-key\",\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  // ---- Google AI Gemini provider --------------------------------------------

  @Test
  void toApiAgent_googleAiGemini_allFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            """
        {"model":"googleaigemini","modelName":"gemini-pro",
         "apiKey":"gai-test-key","temperature":0.6,"topP":0.7,"maxTokens":1500,
         "systemPrompt":"test"}""");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }

  @Test
  void toApiAgent_googleAiGemini_minimalFields() throws Exception {
    JsonNode node =
        JSON.readTree(
            "{\"model\":\"gemini\",\"modelName\":\"gemini-1.5-pro\",\"apiKey\":\"gai-key\",\"systemPrompt\":\"test\"}");
    Agent result = AgentConverter.toApiAgent(node);
    assertThat(result).isNotNull();
  }
}
