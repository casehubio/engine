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
package io.casehub.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.casehub.platform.api.governance.BackoffStrategy;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.platform.api.governance.RetryPolicy;
import io.casehub.worker.api.Worker;
import io.smallrye.mutiny.Multi;
import java.time.Duration;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class AgentWorkerTest {

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
        "test system prompt",
        null,
        UnaryOperator.identity(),
        UnaryOperator.identity(),
        null,
        null,
        Duration.ofSeconds(30));
  }

  @Test
  void workerCanBeCreatedWithAgent() {
    var invoker = stubInvoker("{\"output\": \"processed\"}");

    Worker worker =
        Worker.builder()
            .name("ai-text-processor")
            .capabilityName("text-processing")
            .function(new AgentWorkerFunction(invoker))
            .description("AI-powered text processing worker")
            .build();

    assertEquals("ai-text-processor", worker.name());
    assertEquals("AI-powered text processing worker", worker.description());
    assertEquals(1, worker.capabilities().size());
    assertEquals("text-processing", worker.capabilities().iterator().next());

    assertNotNull(worker.function());
    assertInstanceOf(AgentWorkerFunction.class, worker.function());
  }

  @Test
  void workerExecutesAgentCorrectly() {
    var invoker = stubInvoker("{\"result\": \"HELLO WORLD\"}");

    Worker worker =
        Worker.builder()
            .name("uppercase-transformer")
            .capabilityName("transform")
            .function(new AgentWorkerFunction(invoker))
            .build();

    WorkerAgentInvoker extracted = ((AgentWorkerFunction) worker.function()).invoker();
    Map<String, Object> result = extracted.execute(Map.of("text", "hello world")).output();

    assertEquals("HELLO WORLD", result.get("result"));
  }

  @Test
  void agentWorkerSupportsExecutionPolicy() {
    var invoker = stubInvoker("{}");

    ExecutionPolicy policy =
        new ExecutionPolicy(5000, new RetryPolicy(3, 1000, BackoffStrategy.EXPONENTIAL));

    Worker worker =
        Worker.builder()
            .name("policy-test")
            .capabilityName("test")
            .function(new AgentWorkerFunction(invoker))
            .executionPolicy(policy)
            .build();

    assertEquals(5000, worker.executionPolicy().timeoutMs());
    assertEquals(3, worker.executionPolicy().retries().maxAttempts());
    assertEquals(BackoffStrategy.EXPONENTIAL, worker.executionPolicy().retries().backoffStrategy());
  }
}
