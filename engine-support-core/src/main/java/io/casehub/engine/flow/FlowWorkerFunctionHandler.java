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
package io.casehub.engine.flow;

import io.casehub.api.model.PlaybookExecutionResult;
import io.casehub.api.model.StepResult;
import io.casehub.api.model.WorkerContext;
import io.casehub.engine.common.internal.executor.ExecutionMetadata;
import io.casehub.engine.common.internal.executor.HandlerResult;
import io.casehub.engine.common.internal.executor.WorkerFunctionHandler;
import io.casehub.worker.api.PlannedAction;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowInstance;
import io.serverlessworkflow.impl.WorkflowModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * {@link WorkerFunctionHandler} for Serverless Workflow workers.
 *
 * <p>Replaces {@code FlowWorkerExecutor} — merges execution logic into the handler model. Sets
 * {@link WorkerExecutionContext} around the workflow execution so casehub tasks inside the workflow
 * can access case context via {@code WorkerExecutionContext.current()}.
 *
 * <p>Plain {@code @ApplicationScoped} (no {@code @DefaultBean}) — when {@code casehub-engine-flow}
 * is on the classpath, this handler is discovered automatically.
 */
public class FlowWorkerFunctionHandler implements WorkerFunctionHandler {

  private final WorkflowApplication app;
  private final FlowExecutionRegistry registry;
  private final ExecutorService virtualThreads;

  public FlowWorkerFunctionHandler(
      final WorkflowApplication app,
      final FlowExecutionRegistry registry,
      final ExecutorService virtualThreads) {
    this.app = app;
    this.registry = registry;
    this.virtualThreads = virtualThreads;
  }

  @Override
  public boolean supports(final WorkerFunction<?, ?> function) {
    return function instanceof SWFWorkerFunction;
  }

  @SuppressWarnings("unchecked")
  @Override
  public HandlerResult execute(
      final WorkerFunction<?, ?> function,
      final Object inputData,
      final WorkerContext context,
      final int timeoutMs,
      final ExecutionMetadata metadata) {
    final SWFWorkerFunction swf = (SWFWorkerFunction) function;
    final Map<String, Object> mapInput = (Map<String, Object>) inputData;

    final WorkflowInstance wfInstance = app.workflowDefinition(swf.workflow()).instance(mapInput);
    final String instanceId = wfInstance.id();

    registry.register(
        instanceId, context.caseId(), metadata.workerName(), metadata.inputDataHash());
    try {
      final CompletableFuture<WorkflowModel> future = wfInstance.start();

      WorkflowModel model;
      try {
        model = future.join();
      } catch (Exception e) {
        List<StepResult> steps = registry.getSteps(instanceId);
        registry.remove(instanceId);
        Throwable root = e.getCause() != null ? e.getCause() : e;
        throw new PlaybookExecutionException(
            root.getMessage(), root, new PlaybookExecutionResult.Failure(root.getMessage(), steps));
      }

      List<StepResult> steps = registry.getSteps(instanceId);
      registry.remove(instanceId);

      Map<String, Object> outputMap =
          model
              .asMap()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "Workflow produced non-serializable model for worker: "
                              + metadata.workerName()));

      Function<Map<String, Object>, PlannedAction> plannedActionFn = null;
      if (swf instanceof FlowWorkerFunction flow) {
        plannedActionFn = flow.plannedActionFn();
      } else if (swf instanceof PlaybookWorkerFunction pb) {
        plannedActionFn = pb.plannedActionFn();
      }

      PlaybookExecutionResult execResult = new PlaybookExecutionResult.Success(outputMap, steps);

      if (plannedActionFn != null) {
        return new HandlerResult(
            WorkerResult.of(outputMap, plannedActionFn.apply(mapInput)), Map.of(), execResult);
      }

      return new HandlerResult(WorkerResult.of(outputMap), Map.of(), execResult);
    } catch (PlaybookExecutionException e) {
      throw e;
    } catch (final RuntimeException e) {
      registry.remove(instanceId);
      throw e;
    }
  }
}
