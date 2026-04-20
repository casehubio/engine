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
package io.casehub.blackboard.stage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for Stage lifecycle transitions and containment. See casehubio/engine#76. CMMN 1.1
 * §5.4.4 alignment: casehubio/engine#84.
 */
class StageTest {

  @Test
  void new_stage_is_pending() {
    assertThat(Stage.create("intake").getStatus()).isEqualTo(StageStatus.PENDING);
  }

  @Test
  void activate_from_pending_transitions_to_active() {
    Stage s = Stage.create("intake");
    s.activate();
    assertThat(s.getStatus()).isEqualTo(StageStatus.ACTIVE);
  }

  @Test
  void activate_from_non_pending_is_noop() {
    Stage s = Stage.create("intake");
    s.activate();
    s.complete();
    s.activate(); // should be noop
    assertThat(s.getStatus()).isEqualTo(StageStatus.COMPLETED);
  }

  @Test
  void complete_from_active_transitions_to_completed() {
    Stage s = Stage.create("intake");
    s.activate();
    s.complete();
    assertThat(s.getStatus()).isEqualTo(StageStatus.COMPLETED);
  }

  @Test
  void terminate_from_active_transitions_to_terminated() {
    Stage s = Stage.create("intake");
    s.activate();
    s.terminate();
    assertThat(s.getStatus()).isEqualTo(StageStatus.TERMINATED);
  }

  @Test
  void suspend_and_resume_roundtrip() {
    Stage s = Stage.create("intake");
    s.activate();
    s.suspend();
    assertThat(s.getStatus()).isEqualTo(StageStatus.SUSPENDED);
    s.resume();
    assertThat(s.getStatus()).isEqualTo(StageStatus.ACTIVE);
  }

  @Test
  void isTerminal_true_for_completed_terminated_faulted() {
    Stage completed = Stage.create("a");
    completed.activate();
    completed.complete();
    Stage terminated = Stage.create("b");
    terminated.activate();
    terminated.terminate();
    assertThat(completed.isTerminal()).isTrue();
    assertThat(terminated.isTerminal()).isTrue();
    assertThat(Stage.create("c").isTerminal()).isFalse();
  }

  @Test
  void containment_addPlanItem_and_addRequiredItem() {
    Stage s = Stage.create("intake");
    s.addPlanItem("pi-1");
    s.addRequiredItem("pi-1");
    assertThat(s.getContainedPlanItemIds()).contains("pi-1");
    assertThat(s.getRequiredItemIds()).contains("pi-1");
  }

  @Test
  void containment_add_same_id_twice_produces_no_duplicates() {
    Stage s = Stage.create("intake");
    s.addPlanItem("pi-1");
    s.addPlanItem("pi-1");
    s.addRequiredItem("pi-1");
    s.addRequiredItem("pi-1");
    assertThat(s.getContainedPlanItemIds()).containsExactly("pi-1");
    assertThat(s.getRequiredItemIds()).containsExactly("pi-1");
  }

  @Test
  void autocomplete_defaults_true() {
    assertThat(Stage.create("intake").isAutocomplete()).isTrue();
  }

  @Test
  void manualActivation_defaults_false() {
    assertThat(Stage.create("intake").isManualActivation()).isFalse();
  }

  @Test
  void complete_from_suspended_transitions_to_completed() {
    Stage s = Stage.create("intake");
    s.activate();
    s.suspend();
    s.complete();
    assertThat(s.getStatus()).isEqualTo(StageStatus.COMPLETED);
  }

  @Test
  void terminate_from_suspended_transitions_to_terminated() {
    Stage s = Stage.create("intake");
    s.activate();
    s.suspend();
    s.terminate();
    assertThat(s.getStatus()).isEqualTo(StageStatus.TERMINATED);
  }

  @Test
  void isTerminal_true_for_faulted() {
    Stage s = Stage.create("x");
    s.fault();
    assertThat(s.isTerminal()).isTrue();
  }
}
