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
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handler for {@link WorkerFunction.Sync} and {@link WorkerFunction.ExchangeProcessor}. Delegates
 * execution to {@link io.casehub.worker.runtime.WorkerExecutor} which provides timeout (SmallRye
 * Guard), retry, schema validation, and OTel tracing. Engine-specific concerns (WorkerRuntime
 * creation, scoped worker sessions) are handled before delegation.
 *
 * <p>The Worker's function is wrapped to inject the engine-created {@link
 * io.casehub.api.engine.WorkerRuntime} as the scope parameter, bridging engine context into the
 * worker executor's execution path without changing its API.
 *
 * <p>Refs casehubio/engine#726, casehubio/engine#567.
 */
@ApplicationScoped
public class SyncWorkerFunctionHandler implements WorkerFunctionHandler {

  private final io.casehub.worker.runtime.WorkerExecutor workerExecutor;
  private final WorkerRuntimeFactory workerRuntimeFactory;
  private final ScopedWorkerRegistry scopedWorkerRegistry;

  @Inject
  public SyncWorkerFunctionHandler(
      io.casehub.worker.runtime.WorkerExecutor workerExecutor,
      WorkerRuntimeFactory workerRuntimeFactory,
      ScopedWorkerRegistry scopedWorkerRegistry) {
    this.workerExecutor = workerExecutor;
    this.workerRuntimeFactory = workerRuntimeFactory;
    this.scopedWorkerRegistry = scopedWorkerRegistry;
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

    Worker effectiveWorker = wrapFunction(worker, runtime);

    try {
      WorkerResult<?> result =
          workerExecutor.execute(effectiveWorker, capability, inputData).await().indefinitely();

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
    } finally {
      if (bindingLock != null) {
        bindingLock.unlock();
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Worker wrapFunction(Worker worker, io.casehub.api.engine.WorkerRuntime runtime) {
    WorkerFunction<?, ?> wrapped;
    if (worker.function() instanceof WorkerFunction.Sync sync) {
      java.util.function.BiFunction originalFn = (java.util.function.BiFunction) sync.fn();
      java.util.function.BiFunction wrappedFn =
          (input, _ignored) -> originalFn.apply(input, runtime);
      wrapped = new WorkerFunction.Sync(sync.inputType(), sync.outputType(), wrappedFn);
    } else if (worker.function() instanceof WorkerFunction.ExchangeProcessor ep) {
      java.util.function.BiFunction originalFn = (java.util.function.BiFunction) ep.fn();
      java.util.function.BiFunction wrappedFn =
          (input, _ignored) -> originalFn.apply(input, runtime);
      wrapped = new WorkerFunction.ExchangeProcessor(ep.inputType(), ep.outputType(), wrappedFn);
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
}
