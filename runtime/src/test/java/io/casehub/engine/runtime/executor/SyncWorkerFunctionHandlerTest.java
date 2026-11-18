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

import io.casehub.api.model.WorkerContext;
import io.casehub.engine.common.internal.executor.ExecutionMetadata;
import io.casehub.engine.common.internal.executor.WorkerFunctionHandler;
import io.casehub.engine.common.internal.worker.scope.ScopedWorkerRegistry;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Exchange;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.smallrye.mutiny.Uni;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncWorkerFunctionHandlerTest {

  private WorkerFunctionHandler handler;

  @BeforeEach
  void setUp() {
    WorkerRuntimeFactory mockFactory =
        new WorkerRuntimeFactory(null, null, null, null, null, null, null, null, null, null) {
          @Override
          public io.casehub.api.engine.WorkerRuntime create(
              UUID caseId,
              String taskId,
              WorkerContext context,
              Map<String, Object> accumulatedState) {
            return new DefaultWorkerRuntime(
                caseId, taskId, context, accumulatedState, null, null, null, null, null, null);
          }
        };

    @SuppressWarnings({"unchecked", "rawtypes"})
    io.casehub.worker.runtime.WorkerExecutor mockExecutor =
        (worker, capability, input) -> {
          if (worker.function() instanceof WorkerFunction.Sync sync) {
            WorkerResult result =
                (WorkerResult) ((java.util.function.BiFunction) sync.fn()).apply(input, null);
            return Uni.createFrom().item(result);
          }
          if (worker.function() instanceof WorkerFunction.ExchangeProcessor ep) {
            WorkerResult result =
                (WorkerResult) ((java.util.function.BiFunction) ep.fn()).apply(input, null);
            return Uni.createFrom().item(result);
          }
          return Uni.createFrom()
              .failure(
                  new UnsupportedOperationException(
                      "Unsupported: " + worker.function().getClass().getName()));
        };

    handler = new SyncWorkerFunctionHandler(mockExecutor, mockFactory, new ScopedWorkerRegistry());
  }

  @Test
  void supports_sync() {
    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of()));
    assertThat(handler.supports(sync)).isTrue();
  }

  @Test
  void supports_exchange_processor() {
    var ep =
        new WorkerFunction.ExchangeProcessor<>(
            String.class, String.class, (exchange, scope) -> WorkerResult.of(exchange));
    assertThat(handler.supports(ep)).isTrue();
  }

  @Test
  void does_not_support_unknown() {
    WorkerFunction<?, ?> unknown =
        new WorkerFunction<Void, Void>() {
          @Override
          public Class<Void> inputType() {
            return Void.class;
          }

          @Override
          public Class<Void> outputType() {
            return Void.class;
          }
        };
    assertThat(handler.supports(unknown)).isFalse();
  }

  @Test
  void sync_function_receives_engine_runtime_despite_delegation() {
    var runtimeRef = new AtomicReference<io.casehub.worker.api.WorkerScope>();
    var sync =
        new WorkerFunction.Sync<>(
            Map.class,
            Map.class,
            (input, scope) -> {
              runtimeRef.set(scope);
              return WorkerResult.of(Map.of("result", "ok"));
            });
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);
    Capability capability = Capability.of("cap", "{}", "{}");

    handler.execute(
        worker, capability, Map.of(), testContext(), new ExecutionMetadata("test-worker", "hash"));

    assertThat(runtimeRef.get()).isNotNull();
    assertThat(runtimeRef.get()).isInstanceOf(io.casehub.api.engine.WorkerRuntime.class);
  }

  @Test
  void executes_sync_function() {
    var sync =
        new WorkerFunction.Sync<>(
            Map.class,
            Map.class,
            (input, scope) -> WorkerResult.of(Map.of("result", input.get("key"))));
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);
    Capability capability = Capability.of("cap", "{}", "{}");

    WorkerResult<?> result =
        handler
            .execute(
                worker,
                capability,
                Map.of("key", "value"),
                testContext(),
                new ExecutionMetadata("w1", "hash1"))
            .result();

    @SuppressWarnings("unchecked")
    Map<String, Object> output = (Map<String, Object>) result.output();
    assertThat(output).containsEntry("result", "value");
  }

  @Test
  void executes_exchange_processor() {
    var ep =
        new WorkerFunction.ExchangeProcessor<>(
            String.class,
            String.class,
            (exchange, scope) ->
                WorkerResult.of(
                    exchange.withBody("processed: " + exchange.body()).withHeader("step", "done")));

    Exchange<String> input = Exchange.of("raw-data", Map.of("correlationId", "abc"));

    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), ep, new ExecutionPolicy(5000, null, null), null, null);
    Capability capability = Capability.of("cap", "{}", "{}");

    WorkerResult<?> result =
        handler
            .execute(worker, capability, input, testContext(), new ExecutionMetadata("w1", "hash1"))
            .result();

    @SuppressWarnings("unchecked")
    Exchange<String> output = (Exchange<String>) result.output();
    assertThat(output.body()).isEqualTo("processed: raw-data");
    assertThat(output.headers()).containsEntry("correlationId", "abc");
    assertThat(output.headers()).containsEntry("step", "done");
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
}
