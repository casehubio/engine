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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.worker.api.WorkerResult;
import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class WorkerAgentInvokerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private AgentProvider stubProvider(String responseText) {
    return new AgentProvider() {
      @Override
      public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        return Multi.createFrom()
            .items(
                new AgentEvent.TextDelta(responseText),
                new AgentEvent.InvocationComplete(
                    10, 20, 0, 0, 0, null, 100L, 0L, null, 0, false, null, null));
      }

      @Override
      public AgentSession openSession(AgentSessionInit init) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Test
  void executeWithSimplePrompt() {
    var provider = stubProvider("{\"answer\": \"hello\"}");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "You are helpful.",
            null,
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    WorkerResult<Map<String, Object>> result = invoker.execute(Map.of("prompt", "say hello"));

    assertEquals("hello", result.output().get("answer"));
  }

  @Test
  void executeDetailedReturnsInvocationResult() {
    var provider = stubProvider("{\"value\": 42}");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            null,
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    WorkerAgentResponse response = invoker.executeDetailed(Map.of("input", "test"));

    assertNotNull(response.invocationResult());
    assertEquals(10, response.invocationResult().inputTokens());
    assertEquals(20, response.invocationResult().outputTokens());
  }

  @Test
  void templateSubstitution() {
    var provider = stubProvider("{\"result\": \"ok\"}");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            "Hello {{name}}, you are {{role}}",
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    invoker.execute(Map.of("name", "Alice", "role", "admin"));
  }

  @Test
  void templateSubstitutionThrowsOnMissingKey() {
    var provider = stubProvider("{\"result\": \"ok\"}");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            "Hello {{name}}, you are {{role}}",
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    assertThrows(AgentException.class, () -> invoker.execute(Map.of("name", "Alice")));
  }

  @Test
  void inputProjectionApplied() {
    var provider = stubProvider("{\"out\": \"projected\"}");
    var jqTransformer = new JqTransformer(".data");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            null,
            jqTransformer::apply,
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    WorkerResult<Map<String, Object>> result =
        invoker.execute(Map.of("data", Map.of("key", "value")));

    assertEquals("projected", result.output().get("out"));
  }

  @Test
  void outputProjectionApplied() {
    var provider = stubProvider("{\"wrapper\": {\"inner\": \"deep\"}}");
    var jqTransformer = new JqTransformer(".wrapper");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            null,
            UnaryOperator.identity(),
            jqTransformer::apply,
            null,
            null,
            null,
            TIMEOUT);

    WorkerResult<Map<String, Object>> result = invoker.execute(Map.of("prompt", "test"));

    assertEquals("deep", result.output().get("inner"));
  }

  @Test
  void emptyResponseThrows() {
    var provider = stubProvider("");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            null,
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    AgentException ex =
        assertThrows(AgentException.class, () -> invoker.execute(Map.of("prompt", "test")));
    assertTrue(ex.getMessage().contains("empty response"));
  }

  @Test
  void invalidJsonResponseThrows() {
    var provider = stubProvider("not json at all");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            null,
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            null,
            TIMEOUT);

    AgentException ex =
        assertThrows(AgentException.class, () -> invoker.execute(Map.of("prompt", "test")));
    assertTrue(ex.getMessage().contains("invalid JSON"));
  }

  @Test
  void plannedActionExtracted() {
    var provider = stubProvider("{\"action\": \"deploy\", \"target\": \"prod\"}");
    var invoker =
        new WorkerAgentInvoker(
            provider,
            "system",
            null,
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            output ->
                io.casehub.worker.api.PlannedAction.of(
                    "Containment: " + output.get("action"), (String) output.get("action"), output),
            null,
            TIMEOUT);

    WorkerResult<Map<String, Object>> result = invoker.execute(Map.of("prompt", "test"));

    assertInstanceOf(io.casehub.worker.api.WorkerOutcome.Success.class, result.outcome());
    var success =
        (io.casehub.worker.api.WorkerOutcome.Success<Map<String, Object>>) result.outcome();
    assertNotNull(success.plannedAction());
    assertEquals("deploy", success.plannedAction().actionType());
  }
}
