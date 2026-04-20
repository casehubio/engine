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

import io.casehub.blackboard.stage.Stage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DefaultCasePlanModel agenda management and milestone tracking. See
 * casehubio/engine#76. Milestone/Goal/Stage alignment: casehubio/engine#84.
 */
class DefaultCasePlanModelTest {

  private DefaultCasePlanModel plan;

  @BeforeEach
  void setUp() {
    plan = new DefaultCasePlanModel(UUID.randomUUID());
  }

  @Test
  void agenda_returns_only_pending_items_sorted_by_priority() {
    PlanItem low = PlanItem.create("b-low", "w-low", 1);
    PlanItem high = PlanItem.create("b-high", "w-high", 10);
    PlanItem running = PlanItem.create("b-run", "w-run", 99);
    running.setStatus(PlanItem.PlanItemStatus.RUNNING);

    plan.addPlanItem(low);
    plan.addPlanItem(high);
    plan.addPlanItem(running);

    List<PlanItem> agenda = plan.getAgenda();
    assertThat(agenda).hasSize(2);
    assertThat(agenda.get(0).getBindingName()).isEqualTo("b-high");
    assertThat(agenda.get(1).getBindingName()).isEqualTo("b-low");
  }

  @Test
  void getTopPlanItems_respects_limit() {
    for (int i = 0; i < 5; i++) {
      plan.addPlanItem(PlanItem.create("b-" + i, "w-" + i, i));
    }
    assertThat(plan.getTopPlanItems(3)).hasSize(3);
  }

  @Test
  void getTopPlanItems_handles_limit_larger_than_agenda() {
    plan.addPlanItem(PlanItem.create("b-a", "w-a", 0));
    assertThat(plan.getTopPlanItems(100)).hasSize(1);
  }

  @Test
  void getPlanItem_returns_by_id() {
    PlanItem item = PlanItem.create("b-a", "w-a", 0);
    plan.addPlanItem(item);
    assertThat(plan.getPlanItem(item.getPlanItemId())).contains(item);
  }

  @Test
  void removePlanItem_removes_from_agenda() {
    PlanItem item = PlanItem.create("b-a", "w-a", 0);
    plan.addPlanItem(item);
    plan.removePlanItem(item.getPlanItemId());
    assertThat(plan.getAgenda()).isEmpty();
  }

  @Test
  void milestone_lifecycle_pending_to_achieved() {
    plan.trackMilestone("docs-received");
    assertThat(plan.isMilestoneAchieved("docs-received")).isFalse();
    plan.achieveMilestone("docs-received");
    assertThat(plan.isMilestoneAchieved("docs-received")).isTrue();
  }

  @Test
  void achieve_untracked_milestone_does_not_throw() {
    plan.achieveMilestone("unknown");
    assertThat(plan.isMilestoneAchieved("unknown")).isFalse();
  }

  @Test
  void focus_and_rationale_roundtrip() {
    plan.setFocus("analysis");
    plan.setFocusRationale("high-value documents detected");
    assertThat(plan.getFocus()).contains("analysis");
    assertThat(plan.getFocusRationale()).contains("high-value documents detected");
  }

  @Test
  void extensible_kv_roundtrip() {
    plan.put("custom-key", 42);
    assertThat(plan.get("custom-key", Integer.class)).contains(42);
  }

  @Test
  void hasActivePlanItem_true_for_pending_item() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    plan.addPlanItem(item);
    assertThat(plan.hasActivePlanItem("binding-a")).isTrue();
  }

  @Test
  void hasActivePlanItem_true_for_running_item() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    item.setStatus(PlanItem.PlanItemStatus.RUNNING);
    plan.addPlanItem(item);
    assertThat(plan.hasActivePlanItem("binding-a")).isTrue();
  }

  @Test
  void hasActivePlanItem_false_for_completed_item() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    item.setStatus(PlanItem.PlanItemStatus.COMPLETED);
    plan.addPlanItem(item);
    assertThat(plan.hasActivePlanItem("binding-a")).isFalse();
  }

  @Test
  void hasActivePlanItem_false_when_no_item() {
    assertThat(plan.hasActivePlanItem("binding-a")).isFalse();
  }

  @Test
  void hasActivePlanItem_false_for_faulted_item() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    item.setStatus(PlanItem.PlanItemStatus.FAULTED);
    plan.addPlanItem(item);
    assertThat(plan.hasActivePlanItem("binding-a")).isFalse();
  }

  @Test
  void stage_management_add_and_retrieve() {
    Stage stage = Stage.create("intake");
    plan.addStage(stage);
    assertThat(plan.getStage(stage.getStageId())).contains(stage);
    assertThat(plan.getAllStages()).containsExactly(stage);
  }

  @Test
  void getPendingStages_and_getActiveStages_filter_by_status() {
    Stage pending = Stage.create("pending-stage");
    Stage active = Stage.create("active-stage");
    active.activate();
    plan.addStage(pending);
    plan.addStage(active);
    assertThat(plan.getPendingStages()).containsExactly(pending);
    assertThat(plan.getActiveStages()).containsExactly(active);
  }
}
