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
package io.casehub.engine.internal.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.context.PropagationContext;
import io.casehub.api.model.CaseChannel;
import io.casehub.api.model.ProvisionContext;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkResult;
import io.casehub.api.model.WorkerContext;
import io.casehub.api.spi.ProvisioningException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for default no-op/empty SPI implementations. Instantiates the real classes (not stubs)
 * to verify actual behaviour.
 */
class DefaultWorkerSpiImplementationsTest {

  // --- NoOpWorkerProvisioner ---

  @Test
  void noOpProvisioner_getCapabilities_returnsEmptySet() {
    var provisioner = new NoOpWorkerProvisioner();
    assertThat(provisioner.getCapabilities()).isEmpty();
  }

  @Test
  void noOpProvisioner_terminate_doesNotThrow() {
    var provisioner = new NoOpWorkerProvisioner();
    assertThatNoException().isThrownBy(() -> provisioner.terminate("any-worker-id"));
  }

  @Test
  void noOpProvisioner_provision_throwsProvisioningException() {
    var provisioner = new NoOpWorkerProvisioner();
    var workerContext =
        new WorkerContext("desc", null, null, List.of(), PropagationContext.createRoot(), Map.of());
    var ctx =
        new ProvisionContext(
            UUID.randomUUID(), "task", workerContext, PropagationContext.createRoot());
    assertThatThrownBy(() -> provisioner.provision(java.util.Set.of("cap"), ctx))
        .isInstanceOf(ProvisioningException.class)
        .hasMessageContaining("WorkerProvisioner");
  }

  // --- NoOpWorkerStatusListener ---

  @Test
  void noOpStatusListener_onWorkerStarted_doesNotThrow() {
    var listener = new NoOpWorkerStatusListener();
    assertThatNoException()
        .isThrownBy(() -> listener.onWorkerStarted("worker-1", Map.of("session", "tmux-123")));
  }

  @Test
  void noOpStatusListener_onWorkerStarted_nullSessionMeta_doesNotThrow() {
    var listener = new NoOpWorkerStatusListener();
    assertThatNoException().isThrownBy(() -> listener.onWorkerStarted("worker-1", null));
  }

  @Test
  void noOpStatusListener_onWorkerCompleted_doesNotThrow() {
    var listener = new NoOpWorkerStatusListener();
    var result = WorkResult.completed("corr-1", Map.of(), "worker-1");
    assertThatNoException().isThrownBy(() -> listener.onWorkerCompleted("worker-1", result));
  }

  @Test
  void noOpStatusListener_onWorkerStalled_doesNotThrow() {
    var listener = new NoOpWorkerStatusListener();
    assertThatNoException().isThrownBy(() -> listener.onWorkerStalled("unknown-worker"));
  }

  // --- NoOpCaseChannelProvider ---

  @Test
  void noOpChannelProvider_openChannel_returnsNonNull() {
    var provider = new NoOpCaseChannelProvider();
    UUID caseId = UUID.randomUUID();
    CaseChannel ch = provider.openChannel(caseId, "coordination");
    assertThat(ch).isNotNull();
    assertThat(ch.backendType()).isEqualTo("none");
    assertThat(ch.purpose()).isEqualTo("coordination");
    assertThat(ch.id()).isNotBlank();
  }

  @Test
  void noOpChannelProvider_listChannels_returnsEmptyList() {
    var provider = new NoOpCaseChannelProvider();
    assertThat(provider.listChannels(UUID.randomUUID())).isEmpty();
  }

  @Test
  void noOpChannelProvider_postToChannel_doesNotThrow() {
    var provider = new NoOpCaseChannelProvider();
    CaseChannel ch = provider.openChannel(UUID.randomUUID(), "p");
    assertThatNoException().isThrownBy(() -> provider.postToChannel(ch, "alice", "hello"));
  }

  @Test
  void noOpChannelProvider_closeChannel_doesNotThrow() {
    var provider = new NoOpCaseChannelProvider();
    CaseChannel ch = provider.openChannel(UUID.randomUUID(), "p");
    assertThatNoException().isThrownBy(() -> provider.closeChannel(ch));
  }

  // --- EmptyWorkerContextProvider ---

  @Test
  void emptyContextProvider_buildContext_taskDescriptionMatchesCapability() {
    var provider = new EmptyWorkerContextProvider();
    WorkerContext ctx = provider.buildContext("worker-1", WorkRequest.of("researcher", Map.of()));
    assertThat(ctx.taskDescription()).isEqualTo("researcher");
  }

  @Test
  void emptyContextProvider_buildContext_priorWorkersIsEmptyNotNull() {
    var provider = new EmptyWorkerContextProvider();
    WorkerContext ctx = provider.buildContext("worker-1", WorkRequest.of("task", Map.of()));
    assertThat(ctx.priorWorkers()).isNotNull().isEmpty();
  }

  @Test
  void emptyContextProvider_buildContext_propagationContextIsNotNull() {
    var provider = new EmptyWorkerContextProvider();
    WorkerContext ctx = provider.buildContext("worker-1", WorkRequest.of("task", Map.of()));
    assertThat(ctx.propagationContext()).isNotNull();
  }
}
