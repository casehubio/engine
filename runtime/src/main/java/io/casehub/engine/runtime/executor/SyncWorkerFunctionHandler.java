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

import io.casehub.api.model.WorkerContext;
import io.casehub.engine.common.internal.executor.ExecutionMetadata;
import io.casehub.engine.common.internal.executor.HandlerResult;
import io.casehub.engine.common.internal.executor.WorkerFunctionHandler;
import io.casehub.engine.common.internal.worker.scope.ScopedWorkerRegistry;
import io.casehub.engine.common.internal.worker.scope.ScopedWorkerSession;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handler for {@link WorkerFunction.Sync} and {@link WorkerFunction.ExchangeProcessor}. Delegates
 * execution to {@link io.casehub.worker.runtime.WorkerExecutor} for OTel tracing, but keeps three
 * engine semantics the executor does not provide on this path:
 *
 * <ul>
 *   <li>timeout — the function runs on a virtual thread and the wait is bounded here, because the
 *       executor's Guard cannot interrupt a synchronous function;
 *   <li>retries — the executor gets a policy without retries and worker exceptions are rethrown, so
 *       the engine's retry orchestration handles them (see {@link #withoutRetries});
 *   <li>schemas — engine capabilities hold JQ projections, not JSON Schemas, so the executor's
 *       schema validation is bypassed (see {@link #withoutSchemas}).
 * </ul>
 *
 * Engine-specific concerns (WorkerRuntime creation, scoped worker sessions) are handled before
 * delegation.
 *
 * <p>The Worker's function is wrapped to inject the engine-created {@link
 * io.casehub.api.engine.WorkerRuntime} as the scope parameter, bridging engine context into the
 * worker executor's execution path without changing its API.
 *
 * <p>Refs casehubio/engine#726, casehubio/engine#567.
 */
@ApplicationScoped
public class SyncWorkerFunctionHandler implements WorkerFunctionHandler {

  private static final String EMPTY_SCHEMA = "{}";

  private final io.casehub.worker.runtime.WorkerExecutor workerExecutor;
  private final WorkerRuntimeFactory workerRuntimeFactory;
  private final ScopedWorkerRegistry scopedWorkerRegistry;
  private final ExecutorService virtualThreads;

  @Inject
  public SyncWorkerFunctionHandler(
      io.casehub.worker.runtime.WorkerExecutor workerExecutor,
      WorkerRuntimeFactory workerRuntimeFactory,
      ScopedWorkerRegistry scopedWorkerRegistry,
      @VirtualThreads ExecutorService virtualThreads) {
    this.workerExecutor = workerExecutor;
    this.workerRuntimeFactory = workerRuntimeFactory;
    this.scopedWorkerRegistry = scopedWorkerRegistry;
    this.virtualThreads = virtualThreads;
  }

  @Override
  public boolean supports(WorkerFunction<?, ?> function) {
    return function instanceof WorkerFunction.Sync
        || function instanceof io.casehub.worker.api.ExchangeAwareFunction;
  }

  @SuppressWarnings("unchecked")
  @Override
  public HandlerResult execute(
      Worker worker,
      Capability capability,
      Object inputData,
      WorkerContext context,
      ExecutionMetadata metadata) {

    if (inputData == null) {
      throw new io.casehub.api.context.BridgeTypeMismatchException(
          worker.function().inputType().getName(), "null");
    }
    if (!worker.function().inputType().isInstance(inputData)) {
      throw new io.casehub.api.context.BridgeTypeMismatchException(
          worker.function().inputType().getName(), inputData.getClass().getName());
    }

    Map<String, Object> accState = Map.of();
    ReentrantLock bindingLock = null;
    if (metadata.executionMode() == io.casehub.api.model.ExecutionMode.REINVOKED
        && metadata.bindingName() != null) {
      var scopeKey = new ScopedWorkerRegistry.ScopeKey(context.caseId(), metadata.bindingName());
      bindingLock = scopedWorkerRegistry.executionLock(scopeKey);
      bindingLock.lock();
      accState =
          scopedWorkerRegistry
              .get(context.caseId(), metadata.bindingName())
              .filter(ScopedWorkerSession.Reinvoked.class::isInstance)
              .map(s -> ((ScopedWorkerSession.Reinvoked) s).accumulatedState().get())
              .orElse(Map.of());
    }

    io.casehub.api.engine.WorkerRuntime runtime =
        workerRuntimeFactory.create(context.caseId(), metadata.workerName(), context, accState);

    AtomicReference<Throwable> workerFailure = new AtomicReference<>();
    Worker effectiveWorker = withoutRetries(wrapFunction(worker, runtime, workerFailure));
    Capability effectiveCapability = withoutSchemas(capability);
    Integer timeoutMs =
        worker.executionPolicy() != null ? worker.executionPolicy().timeoutMs() : null;

    try {
      // The worker executor runs the function synchronously on the subscribing thread, so its
      // Guard timeout cannot fire until the function returns. Run it on a virtual thread and bound
      // the wait here, as the handler did before #726.
      Future<WorkerResult<?>> future =
          virtualThreads.submit(
              () ->
                  workerExecutor
                      .execute(effectiveWorker, effectiveCapability, inputData)
                      .await()
                      .indefinitely());
      WorkerResult<?> result =
          timeoutMs != null ? future.get(timeoutMs, TimeUnit.MILLISECONDS) : future.get();

      // The executor recovers worker exceptions into WorkerResult.failed(...). Rethrow them so the
      // engine's retry path (WORKER_EXECUTION_FAILED, rescheduling, FAULTED, onWorkerStalled) runs.
      rethrowIfFailed(workerFailure.get());

      if (metadata.executionMode() == io.casehub.api.model.ExecutionMode.REINVOKED
          && metadata.bindingName() != null) {
        Map<String, Object> output =
            result.output() instanceof Map ? (Map<String, Object>) result.output() : null;
        if (output != null) {
          scopedWorkerRegistry
              .get(context.caseId(), metadata.bindingName())
              .filter(ScopedWorkerSession.Reinvoked.class::isInstance)
              .ifPresent(s -> ((ScopedWorkerSession.Reinvoked) s).accumulatedState().set(output));
        }
      }
      return new HandlerResult(result);
    } catch (TimeoutException e) {
      return new HandlerResult(WorkerResult.expired("Worker timed out after " + timeoutMs + "ms"));
    } catch (ExecutionException e) {
      rethrowIfFailed(e.getCause());
      throw new IllegalStateException("unreachable");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Worker execution interrupted", e);
    } finally {
      if (bindingLock != null) {
        bindingLock.unlock();
      }
    }
  }

  private static void rethrowIfFailed(Throwable failure) {
    if (failure == null) {
      return;
    }
    if (failure instanceof RuntimeException re) {
      throw re;
    }
    if (failure instanceof Error err) {
      throw err;
    }
    throw new RuntimeException(failure);
  }

  /**
   * Retries are owned by the engine (RetryOrchestrator reschedules the worker task and records
   * WORKER_EXECUTION_FAILED per attempt). The worker executor must not retry on its own as well.
   */
  private static Worker withoutRetries(Worker worker) {
    ExecutionPolicy policy = worker.executionPolicy();
    if (policy == null || policy.retries() == null) {
      return worker;
    }
    return new Worker(
        worker.name(),
        worker.capabilities(),
        worker.function(),
        new ExecutionPolicy(policy.timeoutMs(), null, policy.circuitBreaker()),
        worker.description(),
        worker.definitionRef());
  }

  /**
   * Engine capabilities carry JQ projections in {@code inputProjection}/{@code outputProjection};
   * the engine applies them before and after the handler runs. The worker executor interprets the
   * same fields as JSON Schema and fails on JQ ("Malformed JSON Schema"), so it gets a copy with
   * empty schemas — the engine performed no schema validation on this path before #726 either.
   */
  private static Capability withoutSchemas(Capability capability) {
    return new Capability(capability.name(), EMPTY_SCHEMA, EMPTY_SCHEMA, capability.description());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Worker wrapFunction(
      Worker worker,
      io.casehub.api.engine.WorkerRuntime runtime,
      AtomicReference<Throwable> failure) {
    WorkerFunction<?, ?> wrapped;
    if (worker.function() instanceof WorkerFunction.Sync sync) {
      java.util.function.BiFunction originalFn = (java.util.function.BiFunction) sync.fn();
      wrapped =
          new WorkerFunction.Sync(
              sync.inputType(), sync.outputType(), capturing(originalFn, runtime, failure));
    } else if (worker.function() instanceof WorkerFunction.ExchangeProcessor ep) {
      java.util.function.BiFunction originalFn = (java.util.function.BiFunction) ep.fn();
      wrapped =
          new WorkerFunction.ExchangeProcessor(
              ep.inputType(), ep.outputType(), capturing(originalFn, runtime, failure));
    } else {
      throw new UnsupportedOperationException(
          "Cannot wrap: " + worker.function().getClass().getName());
    }
    return new Worker(
        worker.name(),
        worker.capabilities(),
        wrapped,
        worker.executionPolicy(),
        worker.description(),
        worker.definitionRef());
  }

  /** Injects the engine runtime as scope and records any throwable before rethrowing it. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static java.util.function.BiFunction capturing(
      java.util.function.BiFunction originalFn,
      io.casehub.api.engine.WorkerRuntime runtime,
      AtomicReference<Throwable> failure) {
    return (input, _ignored) -> {
      try {
        return originalFn.apply(input, runtime);
      } catch (RuntimeException | Error e) {
        failure.set(e);
        throw e;
      }
    };
  }
}
