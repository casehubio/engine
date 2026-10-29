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

import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.serverlessworkflow.impl.TaskContextData;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import io.serverlessworkflow.impl.lifecycle.WorkflowExecutionListener;
import java.time.Duration;
import java.time.Instant;

public class PlaybookExecutionListener implements WorkflowExecutionListener {

  private final FlowExecutionRegistry registry;

  public PlaybookExecutionListener(FlowExecutionRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void onTaskCompleted(TaskCompletedEvent event) {
    String instanceId = event.workflowContext().instanceData().id();
    if (registry.find(instanceId).isEmpty()) {
      return;
    }
    TaskContextData task = event.taskContext();
    registry.recordStep(
        instanceId,
        new StepResult(
            task.position().toString(),
            task.taskName(),
            StepStatus.SUCCEEDED,
            elapsed(task.startedAt(), task.completedAt()),
            null));
  }

  @Override
  public void onTaskFailed(TaskFailedEvent event) {
    String instanceId = event.workflowContext().instanceData().id();
    if (registry.find(instanceId).isEmpty()) {
      return;
    }
    TaskContextData task = event.taskContext();
    registry.recordStep(
        instanceId,
        new StepResult(
            task.position().toString(),
            task.taskName(),
            StepStatus.FAILED,
            elapsed(task.startedAt(), task.completedAt()),
            event.cause() != null ? event.cause().getMessage() : "unknown"));
  }

  private Duration elapsed(Instant start, Instant end) {
    if (start == null || end == null) {
      return Duration.ZERO;
    }
    return Duration.between(start, end);
  }
}
