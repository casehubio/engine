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

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry mapping live workflow instance IDs to their {@link FlowExecution} context.
 * Workflow steps run on quarkus-flow's cached thread pool — thread-locals are not usable; a
 * ConcurrentHashMap keyed by instance ID is the correct cross-thread mechanism.
 */
@ApplicationScoped
public class FlowExecutionRegistry {

  private final ConcurrentHashMap<String, FlowExecution> executions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, java.util.List<io.casehub.api.model.StepResult>>
      stepData = new ConcurrentHashMap<>();

  public void register(
      final String instanceId,
      final UUID caseId,
      final String workerName,
      final String inputDataHash) {
    executions.put(instanceId, new FlowExecution(caseId, workerName, inputDataHash));
  }

  public FlowExecution get(final String instanceId) {
    final FlowExecution execution = executions.get(instanceId);
    if (execution == null) {
      throw new IllegalStateException(
          "No FlowExecution registered for workflow instance ID: "
              + instanceId
              + ". Dispatch called outside a workflow step or after cleanup.");
    }
    return execution;
  }

  public java.util.Optional<FlowExecution> find(final String instanceId) {
    return java.util.Optional.ofNullable(executions.get(instanceId));
  }

  public void recordStep(final String instanceId, final io.casehub.api.model.StepResult step) {
    stepData
        .computeIfAbsent(
            instanceId, k -> java.util.Collections.synchronizedList(new java.util.ArrayList<>()))
        .add(step);
  }

  public java.util.List<io.casehub.api.model.StepResult> getSteps(final String instanceId) {
    java.util.List<io.casehub.api.model.StepResult> steps = stepData.get(instanceId);
    return steps != null ? java.util.List.copyOf(steps) : java.util.List.of();
  }

  public void removeSteps(final String instanceId) {
    stepData.remove(instanceId);
  }

  public void remove(final String instanceId) {
    executions.remove(instanceId);
    stepData.remove(instanceId);
  }
}
