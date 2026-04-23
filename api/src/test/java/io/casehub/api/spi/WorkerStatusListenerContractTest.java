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
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkerStatusListenerContractTest {

  @Test
  void interface_hasOnWorkerStartedMethod() throws Exception {
    assertThat(WorkerStatusListener.class.getMethod("onWorkerStarted", String.class, Map.class))
        .isNotNull();
  }

  @Test
  void interface_hasOnWorkerCompletedMethod() throws Exception {
    assertThat(
            WorkerStatusListener.class.getMethod(
                "onWorkerCompleted", String.class, WorkResult.class))
        .isNotNull();
  }

  @Test
  void interface_hasOnWorkerStalledMethod() throws Exception {
    assertThat(WorkerStatusListener.class.getMethod("onWorkerStalled", String.class)).isNotNull();
  }

  @Test
  void noOp_allMethodsAreCallableWithoutException() {
    WorkerStatusListener listener = new NoOpStub();
    WorkResult result = WorkResult.completed("corr-1", Map.of("output", "done"), "worker-1");
    assertThatNoException()
        .isThrownBy(
            () -> {
              listener.onWorkerStarted("worker-1", Map.of("session", "tmux-123"));
              listener.onWorkerCompleted("worker-1", result);
              listener.onWorkerStalled("worker-1");
            });
  }

  @Test
  void noOp_acceptsNullSessionMeta() {
    WorkerStatusListener listener = new NoOpStub();
    assertThatNoException().isThrownBy(() -> listener.onWorkerStarted("worker-1", null));
  }

  @Test
  void noOp_onWorkerStalled_unknownWorker_isNoOp() {
    WorkerStatusListener listener = new NoOpStub();
    assertThatNoException().isThrownBy(() -> listener.onWorkerStalled("unknown-999"));
  }

  static class NoOpStub implements WorkerStatusListener {
    @Override
    public void onWorkerStarted(String workerId, java.util.Map<String, String> sessionMeta) {}

    @Override
    public void onWorkerCompleted(String workerId, WorkResult result) {}

    @Override
    public void onWorkerStalled(String workerId) {}
  }
}
