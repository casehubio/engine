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
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReactiveWorkerContextProviderContractTest {

  @Test
  void noOp_buildContext_returnsNonNullContext() {
    ReactiveWorkerContextProvider provider = new EmptyStub();
    WorkRequest task = WorkRequest.of("researcher", Map.of());
    WorkerContext ctx = provider.buildContext("worker-1", task).await().indefinitely();
    assertThat(ctx).isNotNull();
    assertThat(ctx.priorWorkers()).isEmpty();
    assertThat(ctx.taskDescription()).isEqualTo("researcher");
  }

  @Test
  void interface_hasBuildContextMethod() throws Exception {
    assertThat(
            ReactiveWorkerContextProvider.class.getMethod(
                "buildContext", String.class, WorkRequest.class))
        .isNotNull();
  }

  @Test
  void emptyStub_propagationContext_isNotNull() {
    ReactiveWorkerContextProvider provider = new EmptyStub();
    WorkerContext ctx =
        provider.buildContext("worker-1", WorkRequest.of("task", Map.of())).await().indefinitely();
    assertThat(ctx.propagationContext()).isNotNull();
  }

  @Test
  void robustness_emptyWorkerId_doesNotThrow() {
    ReactiveWorkerContextProvider provider = new EmptyStub();
    assertThatNoException()
        .isThrownBy(
            () ->
                provider.buildContext("", WorkRequest.of("task", Map.of())).await().indefinitely());
  }

  static class EmptyStub implements ReactiveWorkerContextProvider {

    @Override
    public Uni<WorkerContext> buildContext(String workerId, WorkRequest task) {
      return Uni.createFrom()
          .item(
              new WorkerContext(
                  task.capability(),
                  null,
                  null,
                  List.of(),
                  PropagationContext.createRoot(),
                  Map.of()));
    }
  }
}
