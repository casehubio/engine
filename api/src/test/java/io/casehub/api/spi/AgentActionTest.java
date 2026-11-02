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

import io.casehub.api.model.coordination.DecisionType;
import org.junit.jupiter.api.Test;

class AgentActionTest {

  @Test
  void initiate_message_is_collaboration() {
    assertThat(new AgentAction.InitiateMessage("target").decisionType())
        .isEqualTo(DecisionType.COLLABORATION);
  }

  @Test
  void deposit_signal_is_signaling() {
    assertThat(new AgentAction.DepositSignal("alert").decisionType())
        .isEqualTo(DecisionType.SIGNALING);
  }

  @Test
  void select_task_is_task_selection() {
    assertThat(new AgentAction.SelectTask("task-1").decisionType())
        .isEqualTo(DecisionType.TASK_SELECTION);
  }

  @Test
  void request_provisioning_is_provisioning() {
    assertThat(new AgentAction.RequestProvisioning().decisionType())
        .isEqualTo(DecisionType.PROVISIONING);
  }

  @Test
  void mutate_context_is_context_mutation() {
    assertThat(new AgentAction.MutateContext("key").decisionType())
        .isEqualTo(DecisionType.CONTEXT_MUTATION);
  }
}
