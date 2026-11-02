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
package io.casehub.engine.runtime.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.spi.AgentAction;
import io.casehub.api.spi.GuardDecision;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NoOpAgentCapabilityGuardTest {

  private final NoOpAgentCapabilityGuard guard = new NoOpAgentCapabilityGuard();

  @Test
  void always_permits_deposit_signal() {
    GuardDecision decision =
        guard.check(UUID.randomUUID(), "agent-1", new AgentAction.DepositSignal("alert"));
    assertThat(decision).isInstanceOf(GuardDecision.Permitted.class);
  }

  @Test
  void always_permits_select_task() {
    GuardDecision decision =
        guard.check(UUID.randomUUID(), "agent-1", new AgentAction.SelectTask("task-1"));
    assertThat(decision).isInstanceOf(GuardDecision.Permitted.class);
  }

  @Test
  void always_permits_request_provisioning() {
    GuardDecision decision =
        guard.check(UUID.randomUUID(), "agent-1", new AgentAction.RequestProvisioning());
    assertThat(decision).isInstanceOf(GuardDecision.Permitted.class);
  }
}
