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
package io.casehub.api.model.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class DelegationAssignmentTest {

  private static final CoordinationTask TASK = new CoordinationTask("t-1", "analyse", null, null);

  @Test
  void valid_construction() {
    var assignment = new DelegationAssignment("agent-a", TASK);
    assertThat(assignment.agentId()).isEqualTo("agent-a");
    assertThat(assignment.task()).isEqualTo(TASK);
  }

  @Test
  void null_agentId_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DelegationAssignment(null, TASK))
        .withMessageContaining("agentId");
  }

  @Test
  void null_task_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DelegationAssignment("agent-a", null))
        .withMessageContaining("task");
  }
}
