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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TransitionPolicyTest {

  private static final TransitionTrigger.Threshold SWARM_SIZE_TRIGGER =
      new TransitionTrigger.Threshold("swarm.size", 5.0);

  private static final TransitionTrigger.Convergence CONVERGENCE_TRIGGER =
      new TransitionTrigger.Convergence(0.9);

  @Test
  void valid_immediate_non_reversible() {
    var policy =
        new TransitionPolicy(
            TopologyType.CENTRALISED,
            TopologyType.DISTRIBUTED,
            SWARM_SIZE_TRIGGER,
            TransitionStrategy.IMMEDIATE,
            false,
            null);
    assertThat(policy.from()).isEqualTo(TopologyType.CENTRALISED);
    assertThat(policy.to()).isEqualTo(TopologyType.DISTRIBUTED);
    assertThat(policy.trigger()).isEqualTo(SWARM_SIZE_TRIGGER);
    assertThat(policy.strategy()).isEqualTo(TransitionStrategy.IMMEDIATE);
    assertThat(policy.reversible()).isFalse();
    assertThat(policy.rollbackTrigger()).isNull();
  }

  @Test
  void valid_phased_reversible_with_rollback() {
    var rollback = new TransitionTrigger.Convergence(0.3);
    var policy =
        new TransitionPolicy(
            TopologyType.CENTRALISED,
            TopologyType.HYBRID,
            SWARM_SIZE_TRIGGER,
            TransitionStrategy.PHASED,
            true,
            rollback);
    assertThat(policy.reversible()).isTrue();
    assertThat(policy.rollbackTrigger()).isEqualTo(rollback);
  }

  @Test
  void valid_reversible_without_rollback_trigger() {
    var policy =
        new TransitionPolicy(
            TopologyType.CENTRALISED,
            TopologyType.DISTRIBUTED,
            SWARM_SIZE_TRIGGER,
            TransitionStrategy.IMMEDIATE,
            true,
            null);
    assertThat(policy.reversible()).isTrue();
    assertThat(policy.rollbackTrigger()).isNull();
  }

  @Test
  void same_from_and_to_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new TransitionPolicy(
                    TopologyType.CENTRALISED,
                    TopologyType.CENTRALISED,
                    SWARM_SIZE_TRIGGER,
                    TransitionStrategy.IMMEDIATE,
                    false,
                    null))
        .withMessageContaining("from and to must differ");
  }

  @Test
  void rollback_trigger_without_reversible_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new TransitionPolicy(
                    TopologyType.CENTRALISED,
                    TopologyType.DISTRIBUTED,
                    SWARM_SIZE_TRIGGER,
                    TransitionStrategy.IMMEDIATE,
                    false,
                    CONVERGENCE_TRIGGER))
        .withMessageContaining("rollbackTrigger requires reversible");
  }

  @Test
  void null_from_rejected() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new TransitionPolicy(
                    null,
                    TopologyType.DISTRIBUTED,
                    SWARM_SIZE_TRIGGER,
                    TransitionStrategy.IMMEDIATE,
                    false,
                    null));
  }

  @Test
  void null_to_rejected() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new TransitionPolicy(
                    TopologyType.CENTRALISED,
                    null,
                    SWARM_SIZE_TRIGGER,
                    TransitionStrategy.IMMEDIATE,
                    false,
                    null));
  }

  @Test
  void null_trigger_rejected() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new TransitionPolicy(
                    TopologyType.CENTRALISED,
                    TopologyType.DISTRIBUTED,
                    null,
                    TransitionStrategy.IMMEDIATE,
                    false,
                    null));
  }

  @Test
  void null_strategy_rejected() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new TransitionPolicy(
                    TopologyType.CENTRALISED,
                    TopologyType.DISTRIBUTED,
                    SWARM_SIZE_TRIGGER,
                    null,
                    false,
                    null));
  }

  @Test
  void time_trigger_with_phased_strategy() {
    var policy =
        new TransitionPolicy(
            TopologyType.HYBRID,
            TopologyType.DISTRIBUTED,
            new TransitionTrigger.Time(Duration.ofHours(1)),
            TransitionStrategy.PHASED,
            false,
            null);
    assertThat(policy.trigger()).isInstanceOf(TransitionTrigger.Time.class);
    assertThat(policy.strategy()).isEqualTo(TransitionStrategy.PHASED);
  }
}
