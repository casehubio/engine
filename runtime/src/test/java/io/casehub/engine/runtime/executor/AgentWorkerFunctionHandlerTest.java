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
package io.casehub.engine.runtime.executor;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.AgentWorkerFunction;
import io.casehub.api.model.WorkerContext;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.engine.common.internal.executor.ExecutionMetadata;
import io.casehub.engine.common.internal.executor.HandlerResult;
import io.casehub.engine.common.internal.executor.WorkerFunctionHandler;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerOutcome;
import io.casehub.worker.api.WorkerResult;
import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgentWorkerFunctionHandlerTest {

  private WorkerFunctionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new AgentWorkerFunctionHandler(Executors.newVirtualThreadPerTaskExecutor());
  }

  @Test
  void supports_agent() {
    assertThat(handler.supports(new AgentWorkerFunction(testInvoker()))).isTrue();
  }

  @Test
  void does_not_support_sync() {
    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of()));
    assertThat(handler.supports(sync)).isFalse();
  }

  @Test
  void does_not_support_exchange_processor() {
    var ep =
        new WorkerFunction.ExchangeProcessor<>(
            String.class,
            String.class,
            (exchange, scope) -> WorkerResult.of(exchange.withBody("done")));
    assertThat(handler.supports(ep)).isFalse();
  }

  @Test
  @SuppressWarnings("unchecked")
  void executes_agent_function() {
    WorkerAgentInvoker invoker = stubInvoker("{\"agentResult\": \"done\"}");
    AgentWorkerFunction agentFunction = new AgentWorkerFunction(invoker);

    Worker worker =
        new Worker(
            "agent-w",
            Set.of("cap"),
            agentFunction,
            new ExecutionPolicy(5000, null, null),
            null,
            null);
    Capability capability = Capability.of("cap", "{}", "{}");

    HandlerResult handlerResult =
        handler.execute(
            worker,
            capability,
            Map.of("input", "data"),
            testContext(),
            new ExecutionMetadata("agent-w", "hash2"));

    assertThat((Map<String, Object>) handlerResult.result().output())
        .containsEntry("agentResult", "done");
  }

  @Test
  void executes_agent_function_captures_protocol_metadata() {
    WorkerAgentInvoker invoker = stubInvoker("{\"result\": \"ok\"}");
    AgentWorkerFunction agentFunction = new AgentWorkerFunction(invoker);

    Worker worker =
        new Worker(
            "agent-w",
            Set.of("cap"),
            agentFunction,
            new ExecutionPolicy(5000, null, null),
            null,
            null);
    Capability capability = Capability.of("cap", "{}", "{}");

    HandlerResult handlerResult =
        handler.execute(
            worker,
            capability,
            Map.of("input", "data"),
            testContext(),
            new ExecutionMetadata("agent-w", "hash2"));

    assertThat(handlerResult.protocolMetadata()).containsKey("inputTokens");
    assertThat(handlerResult.protocolMetadata()).containsKey("outputTokens");
    assertThat(handlerResult.protocolMetadata()).containsKey("durationMs");
  }

  @Test
  void timeout_produces_expired_outcome() {
    AgentProvider slowProvider =
        new AgentProvider() {
          @Override
          public Multi<AgentEvent> invoke(AgentSessionConfig config) {
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return Multi.createFrom()
                .items(
                    new AgentEvent.TextDelta("{\"result\": \"late\"}"),
                    new AgentEvent.InvocationComplete(
                        0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
          }

          @Override
          public AgentSession openSession(AgentSessionInit init) {
            throw new UnsupportedOperationException();
          }
        };
    WorkerAgentInvoker slowInvoker =
        new WorkerAgentInvoker(
            slowProvider,
            "Slow agent",
            null,
            UnaryOperator.identity(),
            UnaryOperator.identity(),
            null,
            null,
            Duration.ofSeconds(30));
    AgentWorkerFunction agentFunction = new AgentWorkerFunction(slowInvoker);

    Worker worker =
        new Worker(
            "agent-w",
            Set.of("cap"),
            agentFunction,
            new ExecutionPolicy(200, null, null),
            null,
            null);
    Capability capability = Capability.of("cap", "{}", "{}");

    WorkerResult<?> result =
        handler
            .execute(
                worker,
                capability,
                Map.of("input", "data"),
                testContext(),
                new ExecutionMetadata("agent-w", "hash2"))
            .result();

    assertThat(result.outcome()).isInstanceOf(WorkerOutcome.Expired.class);
    assertThat(((WorkerOutcome.Expired) result.outcome()).reason()).contains("200ms");
  }

  private WorkerContext testContext() {
    return new WorkerContext(
        "test-worker",
        UUID.randomUUID(),
        null,
        null,
        io.casehub.api.context.PropagationContext.createRoot(),
        null);
  }

  private WorkerAgentInvoker testInvoker() {
    return stubInvoker("{\"result\": \"ok\"}");
  }

  private WorkerAgentInvoker stubInvoker(String jsonResponse) {
    AgentProvider provider =
        new AgentProvider() {
          @Override
          public Multi<AgentEvent> invoke(AgentSessionConfig config) {
            return Multi.createFrom()
                .items(
                    new AgentEvent.TextDelta(jsonResponse),
                    new AgentEvent.InvocationComplete(
                        0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
          }

          @Override
          public AgentSession openSession(AgentSessionInit init) {
            throw new UnsupportedOperationException();
          }
        };
    return new WorkerAgentInvoker(
        provider,
        "Test agent",
        null,
        UnaryOperator.identity(),
        UnaryOperator.identity(),
        null,
        null,
        Duration.ofSeconds(30));
  }
}
