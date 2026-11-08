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

class TransitionTriggerTest {

  @Test
  void threshold_valid_construction() {
    var trigger = new TransitionTrigger.Threshold("swarm.size", 5.0);
    assertThat(trigger.metric()).isEqualTo("swarm.size");
    assertThat(trigger.value()).isEqualTo(5.0);
  }

  @Test
  void threshold_null_metric_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new TransitionTrigger.Threshold(null, 5.0))
        .withMessageContaining("metric");
  }

  @Test
  void threshold_blank_metric_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TransitionTrigger.Threshold("  ", 5.0))
        .withMessageContaining("metric");
  }

  @Test
  void time_valid_construction() {
    var trigger = new TransitionTrigger.Time(Duration.ofMinutes(10));
    assertThat(trigger.duration()).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  void time_null_duration_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new TransitionTrigger.Time(null))
        .withMessageContaining("duration");
  }

  @Test
  void time_zero_duration_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TransitionTrigger.Time(Duration.ZERO))
        .withMessageContaining("duration");
  }

  @Test
  void time_negative_duration_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TransitionTrigger.Time(Duration.ofMinutes(-1)))
        .withMessageContaining("duration");
  }

  @Test
  void convergence_valid_construction() {
    var trigger = new TransitionTrigger.Convergence(0.85);
    assertThat(trigger.threshold()).isEqualTo(0.85);
  }

  @Test
  void convergence_below_zero_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TransitionTrigger.Convergence(-0.1))
        .withMessageContaining("threshold");
  }

  @Test
  void convergence_above_one_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TransitionTrigger.Convergence(1.1))
        .withMessageContaining("threshold");
  }

  @Test
  void convergence_boundary_zero_accepted() {
    var trigger = new TransitionTrigger.Convergence(0.0);
    assertThat(trigger.threshold()).isEqualTo(0.0);
  }

  @Test
  void convergence_boundary_one_accepted() {
    var trigger = new TransitionTrigger.Convergence(1.0);
    assertThat(trigger.threshold()).isEqualTo(1.0);
  }

  @Test
  void sealed_interface_permits_three_variants() {
    assertThat(TransitionTrigger.class.getPermittedSubclasses())
        .hasSize(3)
        .extracting(Class::getSimpleName)
        .containsExactlyInAnyOrder("Threshold", "Time", "Convergence");
  }
}
