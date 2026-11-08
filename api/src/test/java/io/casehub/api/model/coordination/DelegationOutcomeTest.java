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

class DelegationOutcomeTest {

  @Test
  void valid_construction_completed() {
    var outcome =
        new DelegationOutcome("agent-a", DelegationOutcome.DelegationStatus.COMPLETED, "result");
    assertThat(outcome.agentId()).isEqualTo("agent-a");
    assertThat(outcome.status()).isEqualTo(DelegationOutcome.DelegationStatus.COMPLETED);
    assertThat(outcome.payload()).isEqualTo("result");
  }

  @Test
  void valid_construction_failed_null_payload() {
    var outcome = new DelegationOutcome("agent-b", DelegationOutcome.DelegationStatus.FAILED, null);
    assertThat(outcome.payload()).isNull();
  }

  @Test
  void valid_construction_partial() {
    var outcome =
        new DelegationOutcome("agent-c", DelegationOutcome.DelegationStatus.PARTIAL, "partial");
    assertThat(outcome.status()).isEqualTo(DelegationOutcome.DelegationStatus.PARTIAL);
  }

  @Test
  void null_agentId_rejected() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> new DelegationOutcome(null, DelegationOutcome.DelegationStatus.COMPLETED, "data"))
        .withMessageContaining("agentId");
  }

  @Test
  void null_status_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new DelegationOutcome("agent-a", null, "data"))
        .withMessageContaining("status");
  }
}
