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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.worker.api.PlannedAction;
import io.casehub.worker.api.WorkerResult;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorkerAgentInvoker {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*(.+?)\\s*\\}\\}");
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

  private final AgentProvider agentProvider;
  private final String systemPrompt;
  private final String userMessageTemplate;
  private final UnaryOperator<JsonNode> inputTransformer;
  private final UnaryOperator<JsonNode> outputTransformer;
  private final Function<Map<String, Object>, PlannedAction> plannedActionExtractor;
  private final String model;
  private final Duration timeout;

  public WorkerAgentInvoker(
      AgentProvider agentProvider,
      String systemPrompt,
      String userMessageTemplate,
      UnaryOperator<JsonNode> inputTransformer,
      UnaryOperator<JsonNode> outputTransformer,
      Function<Map<String, Object>, PlannedAction> plannedActionExtractor,
      String model,
      Duration timeout) {
    this.agentProvider = agentProvider;
    this.systemPrompt = systemPrompt;
    this.userMessageTemplate = userMessageTemplate;
    this.inputTransformer = inputTransformer;
    this.outputTransformer = outputTransformer;
    this.plannedActionExtractor = plannedActionExtractor;
    this.model = model;
    this.timeout = timeout;
  }

  static String applyTemplate(String template, Map<String, Object> variables) {
    Matcher matcher = TEMPLATE_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      if (!variables.containsKey(key)) {
        throw new AgentException("Template variable '{{" + key + "}}' not found in input");
      }
      Object value = variables.get(key);
      if (value == null) {
        throw new AgentException("Template variable '{{" + key + "}}' is null");
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  public WorkerResult<Map<String, Object>> execute(Map<String, Object> input) {
    return executeDetailed(input).result();
  }

  public WorkerAgentResponse executeDetailed(Map<String, Object> input) {
    JsonNode inputNode = MAPPER.convertValue(input, JsonNode.class);
    JsonNode transformed = inputTransformer.apply(inputNode);

    String userText;
    if (userMessageTemplate != null) {
      try {
        Map<String, Object> variables = MAPPER.convertValue(transformed, MAP_TYPE);
        userText = applyTemplate(userMessageTemplate, variables);
      } catch (AgentException e) {
        throw e;
      } catch (Exception e) {
        throw new AgentException("Failed to apply userMessage template: " + e.getMessage(), e);
      }
    } else {
      userText = transformed.toString();
    }

    AgentSessionConfig config = AgentSessionConfig.of(systemPrompt, userText);
    if (model != null) {
      config = config.withModel(model);
    }
    AgentInvocation invocationResult;
    try {
      invocationResult =
          AgentInvocation.invoke(
              agentProvider, config, timeout != null ? timeout : DEFAULT_TIMEOUT);
    } catch (Exception e) {
      throw new AgentException("LLM invocation failed: " + e.getMessage(), e);
    }

    String responseText = invocationResult.text();
    if (responseText == null || responseText.isEmpty()) {
      throw new AgentException("LLM returned empty response");
    }

    JsonNode responseJson;
    try {
      responseJson = MAPPER.readTree(responseText);
    } catch (Exception e) {
      throw new AgentException("LLM returned invalid JSON: " + responseText, e);
    }

    Map<String, Object> output =
        MAPPER.convertValue(outputTransformer.apply(responseJson), MAP_TYPE);

    WorkerResult<Map<String, Object>> workerResult;
    if (plannedActionExtractor != null) {
      PlannedAction action = plannedActionExtractor.apply(output);
      workerResult = action != null ? WorkerResult.of(output, action) : WorkerResult.of(output);
    } else {
      workerResult = WorkerResult.of(output);
    }

    return new WorkerAgentResponse(workerResult, invocationResult.completion());
  }

  public String model() {
    return model;
  }
}
