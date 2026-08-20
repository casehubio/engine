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
package io.casehub.engine.planning.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.ExecutorRef;
import io.casehub.api.model.TaskStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasePlanModelPromoteTest {

  private DefaultCasePlanModel plan;

  @BeforeEach
  void setUp() {
    plan = new DefaultCasePlanModel(UUID.randomUUID());
  }

  @Test
  void promotes_primitive_to_compound() {
    var compound =
        PlanItemDefinition.Compound.builder("parent")
            .id("parent")
            .child(
                new PlanItemDefinition.Primitive(
                    "step-a", "Step A", ExecutorRef.of("capA", null), null))
            .child(
                new PlanItemDefinition.Primitive(
                    "step-b", "Step B", ExecutorRef.of("capB", null), null))
            .binding("step-a")
            .binding("step-b")
            .completion(CompletionSemantics.all())
            .build();
    plan.registerDefinition(compound);

    var subCompound =
        PlanItemDefinition.Compound.builder("step-a")
            .id("step-a")
            .child(
                new PlanItemDefinition.Primitive(
                    "sub-1", "Sub 1", ExecutorRef.of("capA1", null), null))
            .child(
                new PlanItemDefinition.Primitive(
                    "sub-2", "Sub 2", ExecutorRef.of("capA2", null), null))
            .binding("sub-1-binding")
            .binding("sub-2-binding")
            .completion(CompletionSemantics.all())
            .build();

    plan.promoteToCompound("step-a", subCompound);

    assertThat(plan.getDefinition("step-a")).isInstanceOf(PlanItemDefinition.Compound.class);
    assertThat(plan.getChildrenOf("step-a")).containsExactlyInAnyOrder("sub-1", "sub-2");
    assertThat(plan.getParentOf("step-a")).contains("parent");
    assertThat(plan.getParentOf("sub-1")).contains("step-a");
    assertThat(plan.getParentOf("sub-2")).contains("step-a");
  }

  @Test
  void throws_when_definition_not_found() {
    assertThatThrownBy(
            () ->
                plan.promoteToCompound(
                    "nonexistent",
                    PlanItemDefinition.Compound.builder("x")
                        .id("x")
                        .completion(CompletionSemantics.all())
                        .build()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void throws_when_target_is_already_compound() {
    var compound =
        PlanItemDefinition.Compound.builder("parent")
            .id("parent")
            .completion(CompletionSemantics.all())
            .build();
    plan.registerDefinition(compound);

    assertThatThrownBy(
            () ->
                plan.promoteToCompound(
                    "parent",
                    PlanItemDefinition.Compound.builder("parent")
                        .id("parent")
                        .completion(CompletionSemantics.all())
                        .build()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void marks_existing_running_plan_item_obsolete() {
    var compound =
        PlanItemDefinition.Compound.builder("parent")
            .id("parent")
            .child(
                new PlanItemDefinition.Primitive(
                    "step-a", "Step A", ExecutorRef.of("capA", null), null))
            .binding("step-a")
            .completion(CompletionSemantics.all())
            .build();
    plan.registerDefinition(compound);

    var item = PlanItem.create("step-a", ExecutorRef.of("capA", null), 0);
    item.tryMarkRunning();
    plan.addPlanItem(item);

    var subCompound =
        PlanItemDefinition.Compound.builder("step-a")
            .id("step-a")
            .child(
                new PlanItemDefinition.Primitive(
                    "sub-1", "Sub 1", ExecutorRef.of("capA1", null), null))
            .binding("sub-1-binding")
            .completion(CompletionSemantics.all())
            .build();

    plan.promoteToCompound("step-a", subCompound);

    var oldItem = plan.getPlanItem(item.getPlanItemId());
    assertThat(oldItem).isPresent();
    assertThat(oldItem.get().getStatus()).isEqualTo(TaskStatus.OBSOLETE);
  }
}
