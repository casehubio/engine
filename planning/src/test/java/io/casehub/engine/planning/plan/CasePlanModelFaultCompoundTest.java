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
import io.casehub.api.model.Participation;
import io.casehub.api.model.TaskStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CasePlanModelFaultCompoundTest {

  private DefaultCasePlanModel createModel() {
    return new DefaultCasePlanModel(UUID.randomUUID());
  }

  @Test
  void faultCompound_cancels_pending_planItems() {
    var model = createModel();
    var compound =
        PlanItemDefinition.Compound.builder("comp-1")
            .id("comp-1")
            .completion(CompletionSemantics.all())
            .binding("step-1", Participation.PARTICIPANT)
            .binding("step-2", Participation.PARTICIPANT)
            .binding("step-3", Participation.PARTICIPANT)
            .build();
    model.registerDefinition(compound);

    var item1 = PlanItem.create("step-1", ExecutorRef.of("worker-1", null), 0);
    var item2 = PlanItem.create("step-2", ExecutorRef.of("worker-2", null), 0);
    var item3 = PlanItem.create("step-3", ExecutorRef.of("worker-3", null), 0);
    model.addPlanItem(item1);
    model.addPlanItem(item2);
    model.addPlanItem(item3);

    item1.tryMarkRunning();
    item1.markCompleted();
    item2.tryMarkRunning();

    model.faultCompound("comp-1");

    assertThat(item1.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(item2.getStatus()).isEqualTo(TaskStatus.RUNNING);
    assertThat(item3.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    assertThat(model.getDefinitionStatus("comp-1")).isEqualTo(TaskStatus.FAULTED);
  }

  @Test
  void faultCompound_rejects_non_compound() {
    var model = createModel();
    var parent =
        PlanItemDefinition.Compound.builder("parent")
            .id("parent")
            .completion(CompletionSemantics.all())
            .child(
                new PlanItemDefinition.Primitive("prim-1", "test", ExecutorRef.of("w", null), null))
            .build();
    model.registerDefinition(parent);
    assertThatThrownBy(() -> model.faultCompound("prim-1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void faultCompound_preserves_completed_and_running() {
    var model = createModel();
    var compound =
        PlanItemDefinition.Compound.builder("comp-1")
            .id("comp-1")
            .completion(CompletionSemantics.all())
            .binding("a", Participation.PARTICIPANT)
            .binding("b", Participation.PARTICIPANT)
            .build();
    model.registerDefinition(compound);

    var itemA = PlanItem.create("a", ExecutorRef.of("w1", null), 0);
    var itemB = PlanItem.create("b", ExecutorRef.of("w2", null), 0);
    model.addPlanItem(itemA);
    model.addPlanItem(itemB);
    itemA.tryMarkRunning();
    itemA.markCompleted();
    itemB.tryMarkRunning();

    model.faultCompound("comp-1");

    assertThat(itemA.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    assertThat(itemB.getStatus()).isEqualTo(TaskStatus.RUNNING);
  }

  @Test
  void hasAnyFaultedParticipant_true_when_faulted() {
    var model = createModel();
    var compound =
        PlanItemDefinition.Compound.builder("comp-1")
            .id("comp-1")
            .completion(CompletionSemantics.all())
            .binding("a", Participation.PARTICIPANT)
            .binding("b", Participation.PARTICIPANT)
            .build();
    model.registerDefinition(compound);

    var itemA = PlanItem.create("a", ExecutorRef.of("w1", null), 0);
    var itemB = PlanItem.create("b", ExecutorRef.of("w2", null), 0);
    model.addPlanItem(itemA);
    model.addPlanItem(itemB);
    itemA.tryMarkRunning();
    itemA.markCompleted();
    itemB.tryMarkRunning();
    itemB.markFaulted();

    assertThat(model.hasAnyFaultedParticipant("comp-1")).isTrue();
  }

  @Test
  void hasAnyFaultedParticipant_false_when_all_completed() {
    var model = createModel();
    var compound =
        PlanItemDefinition.Compound.builder("comp-1")
            .id("comp-1")
            .completion(CompletionSemantics.all())
            .binding("a", Participation.PARTICIPANT)
            .binding("b", Participation.PARTICIPANT)
            .build();
    model.registerDefinition(compound);

    var itemA = PlanItem.create("a", ExecutorRef.of("w1", null), 0);
    var itemB = PlanItem.create("b", ExecutorRef.of("w2", null), 0);
    model.addPlanItem(itemA);
    model.addPlanItem(itemB);
    itemA.tryMarkRunning();
    itemA.markCompleted();
    itemB.tryMarkRunning();
    itemB.markCompleted();

    assertThat(model.hasAnyFaultedParticipant("comp-1")).isFalse();
  }

  @Test
  void hasAnyFaultedParticipant_ignores_companions() {
    var model = createModel();
    var compound =
        PlanItemDefinition.Compound.builder("comp-1")
            .id("comp-1")
            .completion(CompletionSemantics.all())
            .binding("participant", Participation.PARTICIPANT)
            .binding("companion", Participation.COMPANION)
            .build();
    model.registerDefinition(compound);

    var itemP = PlanItem.create("participant", ExecutorRef.of("w1", null), 0);
    var itemC = PlanItem.create("companion", ExecutorRef.of("w2", null), 0);
    model.addPlanItem(itemP);
    model.addPlanItem(itemC);
    itemP.tryMarkRunning();
    itemP.markCompleted();
    itemC.tryMarkRunning();
    itemC.markFaulted();

    assertThat(model.hasAnyFaultedParticipant("comp-1")).isFalse();
  }

  @Test
  void hasAnyFaultedParticipant_detects_cancelled() {
    var model = createModel();
    var compound =
        PlanItemDefinition.Compound.builder("comp-1")
            .id("comp-1")
            .completion(CompletionSemantics.all())
            .binding("a", Participation.PARTICIPANT)
            .build();
    model.registerDefinition(compound);

    var item = PlanItem.create("a", ExecutorRef.of("w1", null), 0);
    model.addPlanItem(item);
    item.markCancelled();

    assertThat(model.hasAnyFaultedParticipant("comp-1")).isTrue();
  }
}
