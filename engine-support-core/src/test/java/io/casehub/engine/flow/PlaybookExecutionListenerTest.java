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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.serverlessworkflow.impl.TaskContext;
import io.serverlessworkflow.impl.WorkflowContext;
import io.serverlessworkflow.impl.WorkflowMutableInstance;
import io.serverlessworkflow.impl.WorkflowPosition;
import io.serverlessworkflow.impl.lifecycle.TaskCompletedEvent;
import io.serverlessworkflow.impl.lifecycle.TaskFailedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaybookExecutionListenerTest {

  private FlowExecutionRegistry registry;
  private PlaybookExecutionListener listener;

  @BeforeEach
  void setUp() {
    registry = new FlowExecutionRegistry();
    listener = new PlaybookExecutionListener(registry);
  }

  @Test
  void records_completed_step() {
    String instanceId = "wf-1";
    registry.register(instanceId, UUID.randomUUID(), "w", "h");

    TaskContext taskCtx =
        mockTaskContext("fetch-data", Instant.now().minusMillis(200), Instant.now());

    listener.onTaskCompleted(new TaskCompletedEvent(mockWorkflowContext(instanceId), taskCtx));

    List<StepResult> steps = registry.getSteps(instanceId);
    assertThat(steps).hasSize(1);
    assertThat(steps.getFirst().stepName()).isEqualTo("fetch-data");
    assertThat(steps.getFirst().status()).isEqualTo(StepStatus.SUCCEEDED);
    assertThat(steps.getFirst().error()).isNull();
  }

  @Test
  void records_failed_step_with_cause() {
    String instanceId = "wf-2";
    registry.register(instanceId, UUID.randomUUID(), "w", "h");

    TaskContext taskCtx =
        mockTaskContext("transform", Instant.now().minusMillis(50), Instant.now());

    listener.onTaskFailed(
        new TaskFailedEvent(
            mockWorkflowContext(instanceId), taskCtx, new RuntimeException("NPE in transform")));

    List<StepResult> steps = registry.getSteps(instanceId);
    assertThat(steps).hasSize(1);
    assertThat(steps.getFirst().status()).isEqualTo(StepStatus.FAILED);
    assertThat(steps.getFirst().error()).contains("NPE in transform");
  }

  @Test
  void ignores_non_casehub_executions() {
    TaskContext taskCtx = mockTaskContext("external-step", Instant.now(), Instant.now());

    listener.onTaskCompleted(new TaskCompletedEvent(mockWorkflowContext("unknown-wf"), taskCtx));

    assertThat(registry.getSteps("unknown-wf")).isEmpty();
  }

  private TaskContext mockTaskContext(String name, Instant startedAt, Instant completedAt) {
    TaskContext ctx = mock(TaskContext.class);
    when(ctx.taskName()).thenReturn(name);
    when(ctx.startedAt()).thenReturn(startedAt);
    when(ctx.completedAt()).thenReturn(completedAt);
    WorkflowPosition pos = mock(WorkflowPosition.class);
    when(pos.toString()).thenReturn("/" + name);
    when(ctx.position()).thenReturn(pos);
    return ctx;
  }

  private WorkflowContext mockWorkflowContext(String instanceId) {
    WorkflowContext ctx = mock(WorkflowContext.class);
    WorkflowMutableInstance instance = mock(WorkflowMutableInstance.class);
    when(instance.id()).thenReturn(instanceId);
    when(ctx.instanceData()).thenReturn(instance);
    return ctx;
  }
}
