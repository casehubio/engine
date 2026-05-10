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
package io.casehub.blackboard.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class PlanItemStatusTest {

  @Test
  void containsExactlyExpectedValues() {
    assertThat(EnumSet.allOf(PlanItemStatus.class))
        .containsExactlyInAnyOrder(
            PlanItemStatus.PENDING,
            PlanItemStatus.ACTIVE,
            PlanItemStatus.COMPLETED,
            PlanItemStatus.TERMINATED,
            PlanItemStatus.FAULTED);
  }

  @Test
  void pendingIsNotTerminalAndNotActive() {
    assertThat(PlanItemStatus.PENDING.isTerminal()).isFalse();
    assertThat(PlanItemStatus.PENDING.isActive()).isFalse();
  }

  @Test
  void activeIsNotTerminal() {
    assertThat(PlanItemStatus.ACTIVE.isTerminal()).isFalse();
    assertThat(PlanItemStatus.ACTIVE.isActive()).isTrue();
  }

  @Test
  void completedTerminatedAndFaultedAreTerminal() {
    assertThat(PlanItemStatus.COMPLETED.isTerminal()).isTrue();
    assertThat(PlanItemStatus.TERMINATED.isTerminal()).isTrue();
    assertThat(PlanItemStatus.FAULTED.isTerminal()).isTrue();
  }

  @Test
  void faultedIsDistinctFromTerminated() {
    assertThat(PlanItemStatus.FAULTED).isNotEqualTo(PlanItemStatus.TERMINATED);
    assertThat(PlanItemStatus.FAULTED.isFaulted()).isTrue();
    assertThat(PlanItemStatus.TERMINATED.isFaulted()).isFalse();
  }
}
