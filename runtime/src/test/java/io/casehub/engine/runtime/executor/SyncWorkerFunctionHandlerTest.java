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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.ExecutionMode;
import io.casehub.api.model.LifecycleScope;
import io.casehub.api.model.Participation;
import io.casehub.api.model.WorkerContext;
import io.casehub.engine.common.internal.executor.ExecutionMetadata;
import io.casehub.engine.common.internal.executor.WorkerExecutionConfig;
import io.casehub.engine.common.internal.executor.WorkerFunctionHandler;
import io.casehub.engine.common.internal.worker.scope.ScopedWorkerRegistry;
import io.casehub.engine.common.internal.worker.scope.ScopedWorkerSession;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.platform.api.governance.RetryPolicy;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Exchange;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerOutcome;
import io.casehub.worker.api.WorkerResult;
import io.smallrye.mutiny.Uni;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncWorkerFunctionHandlerTest {

  private WorkerFunctionHandler handler;
  private ScopedWorkerRegistry registry;
  private AtomicReference<Map<String, Object>> capturedAccState;
  private AtomicReference<Worker> capturedWorker;
  private AtomicReference<Capability> capturedCapability;

  @BeforeEach
  void setUp() {
    capturedAccState = new AtomicReference<>();
    capturedWorker = new AtomicReference<>();
    capturedCapability = new AtomicReference<>();
    WorkerRuntimeFactory mockFactory =
        new WorkerRuntimeFactory(null, null, null, null, null, null, null, null, null, null) {
          @Override
          public io.casehub.api.engine.WorkerRuntime create(
              UUID caseId,
              String taskId,
              WorkerContext context,
              Map<String, Object> accumulatedState) {
            capturedAccState.set(accumulatedState);
            return new DefaultWorkerRuntime(
                caseId, taskId, context, accumulatedState, null, null, null, null, null, null);
          }
        };

    @SuppressWarnings({"unchecked", "rawtypes"})
    io.casehub.worker.runtime.WorkerExecutor mockExecutor =
        (worker, capability, input) -> {
          capturedWorker.set(worker);
          capturedCapability.set(capability);
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

    registry = new ScopedWorkerRegistry();
    this.mockFactory = mockFactory;
    handler = newHandler(mockExecutor);
  }

  private WorkerRuntimeFactory mockFactory;

  private WorkerFunctionHandler newHandler(io.casehub.worker.runtime.WorkerExecutor executor) {
    return new SyncWorkerFunctionHandler(
        executor,
        mockFactory,
        registry,
        java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
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
  void jq_projections_are_not_forwarded_to_worker_executor_as_schemas() {
    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of("result", "ok")));
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);
    // Engine capabilities carry JQ projections; the worker executor parses these fields as
    // JSON Schema and rejects them ("Malformed JSON Schema").
    Capability capability =
        new Capability(
            "cap",
            "{ cbrExperiences: .cbrExperiences }",
            "{ cbrPathAdvice: . }",
            "advises on the investigation path");

    WorkerResult<?> result =
        handler
            .execute(
                worker,
                capability,
                Map.of("cbrExperiences", "none"),
                testContext(),
                new ExecutionMetadata("test-worker", "hash"))
            .result();

    assertThat(result.output()).isEqualTo(Map.of("result", "ok"));
    assertThat(capturedCapability.get().name()).isEqualTo("cap");
    assertThat(capturedCapability.get().description())
        .isEqualTo("advises on the investigation path");
    assertThat(capturedCapability.get().inputProjection()).isEqualTo("{}");
    assertThat(capturedCapability.get().outputProjection()).isEqualTo("{}");
  }

  @Test
  void worker_exception_propagates_although_executor_converts_it_to_failed_result() {
    // casehub-worker's executor recovers every failure into WorkerResult.failed(...). The engine
    // retry path (WORKER_EXECUTION_FAILED, rescheduling, FAULTED) only runs when the handler
    // throws.
    @SuppressWarnings({"unchecked", "rawtypes"})
    io.casehub.worker.runtime.WorkerExecutor recoveringExecutor =
        (worker, capability, input) -> {
          var sync = (WorkerFunction.Sync) worker.function();
          try {
            return Uni.createFrom()
                .item(
                    (WorkerResult) ((java.util.function.BiFunction) sync.fn()).apply(input, null));
          } catch (RuntimeException e) {
            return Uni.createFrom().item(WorkerResult.failed(e.getMessage()));
          }
        };
    WorkerFunctionHandler recoveringHandler = newHandler(recoveringExecutor);
    var sync =
        new WorkerFunction.Sync<Map, Map>(
            Map.class,
            Map.class,
            (input, scope) -> {
              throw new IllegalStateException("Simulated permanent failure");
            });
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);

    assertThatThrownBy(
            () ->
                recoveringHandler.execute(
                    worker,
                    Capability.of("cap", "{}", "{}"),
                    Map.of(),
                    testContext(),
                    new ExecutionMetadata("test-worker", "hash")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Simulated permanent failure");
  }

  @Test
  void worker_executor_does_not_retry_engine_owns_retries() {
    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of("result", "ok")));
    Worker worker =
        new Worker(
            "test-worker",
            Set.of("cap"),
            sync,
            new ExecutionPolicy(5000, new RetryPolicy(3, 100)),
            null,
            null);

    handler.execute(
        worker,
        Capability.of("cap", "{}", "{}"),
        Map.of(),
        testContext(),
        new ExecutionMetadata("test-worker", "hash"));

    assertThat(capturedWorker.get().executionPolicy().retries()).isNull();
    assertThat(capturedWorker.get().executionPolicy().timeoutMs()).isEqualTo(5000);
  }

  @Test
  void slow_worker_expires_after_timeout_without_waiting_for_it() {
    var sync =
        new WorkerFunction.Sync<>(
            Map.class,
            Map.class,
            (input, scope) -> {
              try {
                Thread.sleep(5000);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              return WorkerResult.of(Map.of("result", "too-late"));
            });
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(200, null, null), null, null);

    long start = System.nanoTime();
    WorkerResult<?> result =
        handler
            .execute(
                worker,
                Capability.of("cap", "{}", "{}"),
                Map.of(),
                testContext(),
                new ExecutionMetadata("test-worker", "hash"))
            .result();
    long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

    assertThat(result.outcome()).isInstanceOf(WorkerOutcome.Expired.class);
    assertThat(elapsedMs).isLessThan(2000);
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

  @Test
  void default_timeout_enrichment_reaches_platform_executor() {
    int defaultTimeout = 30000;
    var config = new WorkerExecutionConfig(defaultTimeout);

    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of("done", true)));
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(null, null, null), null, null);

    ExecutionPolicy effectivePolicy = config.withEffectiveTimeout(worker.executionPolicy());
    Worker enrichedWorker =
        new Worker(
            worker.name(),
            worker.capabilities(),
            worker.function(),
            effectivePolicy,
            worker.description(),
            worker.definitionRef());

    Capability capability = Capability.of("cap", "{}", "{}");

    handler.execute(
        enrichedWorker, capability, Map.of(), testContext(), new ExecutionMetadata("w1", "h1"));

    assertThat(enrichedWorker.executionPolicy().timeoutMs()).isEqualTo(defaultTimeout);
    assertThat(capturedWorker.get().executionPolicy().timeoutMs()).isEqualTo(defaultTimeout);
  }

  @Test
  void reinvoked_accumulated_state_round_trip() {
    UUID caseId = UUID.randomUUID();
    String bindingName = "my-binding";
    var initialState = new AtomicReference<>(Map.<String, Object>of("counter", 5));
    var session =
        new ScopedWorkerSession.Reinvoked(
            bindingName,
            caseId,
            "test-worker",
            LifecycleScope.BINDING,
            Participation.PARTICIPANT,
            initialState,
            new AtomicReference<>());
    registry.register(new ScopedWorkerRegistry.ScopeKey(caseId, bindingName), session);

    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of("counter", 6)));
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);
    Capability capability = Capability.of("cap", "{}", "{}");
    var metadata =
        new ExecutionMetadata("test-worker", "hash", bindingName, ExecutionMode.REINVOKED);

    handler.execute(worker, capability, Map.of(), testContext(caseId), metadata);

    assertThat(capturedAccState.get()).containsEntry("counter", 5);
    assertThat(session.accumulatedState().get()).containsEntry("counter", 6);
  }

  @Test
  void reinvoked_binding_lock_released_on_success() {
    UUID caseId = UUID.randomUUID();
    String bindingName = "lock-binding";
    var session =
        new ScopedWorkerSession.Reinvoked(
            bindingName,
            caseId,
            "test-worker",
            LifecycleScope.BINDING,
            Participation.PARTICIPANT,
            new AtomicReference<>(Map.of()),
            new AtomicReference<>());
    var scopeKey = new ScopedWorkerRegistry.ScopeKey(caseId, bindingName);
    registry.register(scopeKey, session);

    var sync =
        new WorkerFunction.Sync<>(
            Map.class, Map.class, (input, scope) -> WorkerResult.of(Map.of("ok", true)));
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);
    Capability capability = Capability.of("cap", "{}", "{}");
    var metadata =
        new ExecutionMetadata("test-worker", "hash", bindingName, ExecutionMode.REINVOKED);

    handler.execute(worker, capability, Map.of(), testContext(caseId), metadata);

    ReentrantLock lock = registry.executionLock(scopeKey);
    assertThat(lock.isLocked()).isFalse();
  }

  @Test
  void reinvoked_binding_lock_released_on_failure() {
    UUID caseId = UUID.randomUUID();
    String bindingName = "fail-binding";
    var session =
        new ScopedWorkerSession.Reinvoked(
            bindingName,
            caseId,
            "test-worker",
            LifecycleScope.BINDING,
            Participation.PARTICIPANT,
            new AtomicReference<>(Map.of()),
            new AtomicReference<>());
    var scopeKey = new ScopedWorkerRegistry.ScopeKey(caseId, bindingName);
    registry.register(scopeKey, session);

    var sync =
        new WorkerFunction.Sync<>(
            Map.class,
            Map.class,
            (input, scope) -> {
              throw new RuntimeException("boom");
            });
    Worker worker =
        new Worker(
            "test-worker", Set.of("cap"), sync, new ExecutionPolicy(5000, null, null), null, null);
    Capability capability = Capability.of("cap", "{}", "{}");
    var metadata =
        new ExecutionMetadata("test-worker", "hash", bindingName, ExecutionMode.REINVOKED);

    assertThatThrownBy(
            () -> handler.execute(worker, capability, Map.of(), testContext(caseId), metadata))
        .isInstanceOf(RuntimeException.class);

    ReentrantLock lock = registry.executionLock(scopeKey);
    assertThat(lock.isLocked()).isFalse();
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

  private WorkerContext testContext(UUID caseId) {
    return new WorkerContext(
        "test-worker",
        caseId,
        null,
        null,
        io.casehub.api.context.PropagationContext.createRoot(),
        null);
  }
}
