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
package io.casehub.blackboard.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseStatus;
import io.casehub.blackboard.plan.PlanItemStatus;
import org.junit.jupiter.api.Test;

class DefaultSubCaseCompletionStrategyTest {

  private final DefaultSubCaseCompletionStrategy strategy = new DefaultSubCaseCompletionStrategy();

  @Test
  void completedMapsToCompleted() {
    assertThat(strategy.resolve(CaseStatus.COMPLETED, null)).isEqualTo(PlanItemStatus.COMPLETED);
  }

  @Test
  void faultedMapsToFaulted() {
    assertThat(strategy.resolve(CaseStatus.FAULTED, null)).isEqualTo(PlanItemStatus.FAULTED);
  }

  @Test
  void cancelledMapsToTerminated() {
    assertThat(strategy.resolve(CaseStatus.CANCELLED, null)).isEqualTo(PlanItemStatus.TERMINATED);
  }

  @Test
  void waitingMapsToTerminated() {
    assertThat(strategy.resolve(CaseStatus.WAITING, null)).isEqualTo(PlanItemStatus.TERMINATED);
  }

  @Test
  void suspendedMapsToTerminated() {
    assertThat(strategy.resolve(CaseStatus.SUSPENDED, null)).isEqualTo(PlanItemStatus.TERMINATED);
  }
}
