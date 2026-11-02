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

import io.casehub.api.model.coordination.DecisionType;

public sealed interface AgentAction {

  DecisionType decisionType();

  record InitiateMessage(String targetAgentId) implements AgentAction {
    @Override
    public DecisionType decisionType() {
      return DecisionType.COLLABORATION;
    }
  }

  record DepositSignal(String signalName) implements AgentAction {
    @Override
    public DecisionType decisionType() {
      return DecisionType.SIGNALING;
    }
  }

  record SelectTask(String taskId) implements AgentAction {
    @Override
    public DecisionType decisionType() {
      return DecisionType.TASK_SELECTION;
    }
  }

  record RequestProvisioning() implements AgentAction {
    @Override
    public DecisionType decisionType() {
      return DecisionType.PROVISIONING;
    }
  }

  record MutateContext(String contextKey) implements AgentAction {
    @Override
    public DecisionType decisionType() {
      return DecisionType.CONTEXT_MUTATION;
    }
  }
}
