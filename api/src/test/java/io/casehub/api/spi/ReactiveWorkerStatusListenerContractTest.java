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

import io.casehub.api.model.WorkResult;
import io.smallrye.mutiny.Uni;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReactiveWorkerStatusListenerContractTest {

  @Test
  void interface_hasOnWorkerStartedMethod() throws Exception {
    assertThat(
            ReactiveWorkerStatusListener.class.getMethod(
                "onWorkerStarted", String.class, Map.class))
        .isNotNull();
  }

  @Test
  void interface_hasOnWorkerCompletedMethod() throws Exception {
    assertThat(
            ReactiveWorkerStatusListener.class.getMethod(
                "onWorkerCompleted", String.class, WorkResult.class))
        .isNotNull();
  }

  @Test
  void interface_hasOnWorkerStalledMethod() throws Exception {
    assertThat(ReactiveWorkerStatusListener.class.getMethod("onWorkerStalled", String.class))
        .isNotNull();
  }

  @Test
  void noOp_onWorkerStarted_completesSuccessfully() {
    ReactiveWorkerStatusListener listener = new NoOpStub();
    assertThatNoException()
        .isThrownBy(
            () ->
                listener
                    .onWorkerStarted("worker-1", Map.of("session", "tmux-123"))
                    .await()
                    .indefinitely());
  }

  @Test
  void noOp_onWorkerStarted_nullSessionMeta_completesSuccessfully() {
    ReactiveWorkerStatusListener listener = new NoOpStub();
    assertThatNoException()
        .isThrownBy(() -> listener.onWorkerStarted("worker-1", null).await().indefinitely());
  }

  @Test
  void noOp_onWorkerCompleted_completesSuccessfully() {
    ReactiveWorkerStatusListener listener = new NoOpStub();
    WorkResult result = WorkResult.completed("corr-1", Map.of("output", "done"), "worker-1");
    assertThatNoException()
        .isThrownBy(() -> listener.onWorkerCompleted("worker-1", result).await().indefinitely());
  }

  @Test
  void noOp_onWorkerStalled_unknownWorker_completesSuccessfully() {
    ReactiveWorkerStatusListener listener = new NoOpStub();
    assertThatNoException()
        .isThrownBy(() -> listener.onWorkerStalled("unknown-999").await().indefinitely());
  }

  static class NoOpStub implements ReactiveWorkerStatusListener {

    @Override
    public Uni<Void> onWorkerStarted(String workerId, Map<String, String> sessionMeta) {
      return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> onWorkerCompleted(String workerId, WorkResult result) {
      return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> onWorkerStalled(String workerId) {
      return Uni.createFrom().voidItem();
    }
  }
}
