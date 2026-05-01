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
package io.casehub.api.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.casehub.api.context.PropagationContext;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkerContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkerContextProviderContractTest {

  @Test
  void interface_hasBuildContextMethod() throws Exception {
    assertThat(
            WorkerContextProvider.class.getMethod(
                "buildContext", String.class, UUID.class, WorkRequest.class))
        .isNotNull();
  }

  @Test
  void happyPath_returnsNonNullContext() {
    WorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("code-reviewer", Map.of("branch", "main"));
    WorkerContext ctx = provider.buildContext("worker-1", UUID.randomUUID(), task);
    assertThat(ctx).isNotNull();
  }

  @Test
  void emptyStub_priorWorkers_isEmptyNotNull() {
    WorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("researcher", Map.of());
    WorkerContext ctx = provider.buildContext("worker-1", UUID.randomUUID(), task);
    assertThat(ctx.priorWorkers()).isNotNull().isEmpty();
  }

  @Test
  void emptyStub_taskDescription_matchesCapability() {
    WorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("security-auditor", Map.of());
    WorkerContext ctx = provider.buildContext("worker-1", UUID.randomUUID(), task);
    assertThat(ctx.taskDescription()).isEqualTo("security-auditor");
  }

  @Test
  void emptyStub_propagationContext_isNotNull() {
    WorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("task", Map.of());
    WorkerContext ctx = provider.buildContext("worker-1", UUID.randomUUID(), task);
    assertThat(ctx.propagationContext()).isNotNull();
  }

  @Test
  void caseId_isReflectedInContext() {
    WorkerContextProvider provider = new EmptyStub();
    UUID caseId = UUID.randomUUID();
    WorkRequest task = WorkRequest.of("task", Map.of());
    WorkerContext ctx = provider.buildContext("worker-1", caseId, task);
    assertThat(ctx.caseId()).isEqualTo(caseId);
  }

  @Test
  void nullCaseId_isHandled() {
    WorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("task", Map.of());
    assertThatNoException().isThrownBy(() -> provider.buildContext("worker-1", null, task));
  }

  @Test
  void robustness_emptyWorkerId_isHandled() {
    WorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("task", Map.of());
    assertThatNoException().isThrownBy(() -> provider.buildContext("", UUID.randomUUID(), task));
  }

  static class EmptyStub implements WorkerContextProvider {
    @Override
    public WorkerContext buildContext(String workerId, UUID caseId, WorkRequest task) {
      return new WorkerContext(
          task.capability(), caseId, null, List.of(), PropagationContext.createRoot(), Map.of());
    }
  }
}
