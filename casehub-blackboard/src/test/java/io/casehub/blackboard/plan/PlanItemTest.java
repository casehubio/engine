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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.plan.PlanElement;
import org.junit.jupiter.api.Test;

class PlanItemTest {

  static class TestElement implements PlanElement {}

  @Test
  void newPlanItemStartsInPending() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    assertThat(item.getStatus()).isEqualTo(PlanItemStatus.PENDING);
  }

  @Test
  void activateTransitionsPendingToActive() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    item.activate();
    assertThat(item.getStatus()).isEqualTo(PlanItemStatus.ACTIVE);
    assertThat(item.getActivatedAt()).isNotNull();
  }

  @Test
  void completeTransitionsActiveToCompleted() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    item.activate();
    item.complete();
    assertThat(item.getStatus()).isEqualTo(PlanItemStatus.COMPLETED);
    assertThat(item.getCompletedAt()).isNotNull();
  }

  @Test
  void terminateTransitionsToTerminated() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    item.activate();
    item.terminate();
    assertThat(item.getStatus()).isEqualTo(PlanItemStatus.TERMINATED);
  }

  @Test
  void faultTransitionsActiveToFaulted() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    item.activate();
    item.fault();
    assertThat(item.getStatus()).isEqualTo(PlanItemStatus.FAULTED);
  }

  @Test
  void activatingAlreadyActiveItemThrows() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    item.activate();
    assertThatThrownBy(item::activate).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void completingPendingItemThrows() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    assertThatThrownBy(item::complete).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void parentReferenceIsNullForRootItem() {
    PlanItem<TestElement> item = PlanItem.of(new TestElement());
    assertThat(item.getParent()).isNull();
  }

  @Test
  void addChildSetsParentOnChild() {
    PlanItem<TestElement> parent = PlanItem.of(new TestElement());
    PlanItem<TestElement> child = PlanItem.of(new TestElement());
    parent.addChild(child);
    assertThat(child.getParent()).isSameAs(parent);
    assertThat(parent.getChildren()).containsExactly(child);
  }

  @Test
  void terminatingParentTerminatesAllChildren() {
    PlanItem<TestElement> parent = PlanItem.of(new TestElement());
    PlanItem<TestElement> child1 = PlanItem.of(new TestElement());
    PlanItem<TestElement> child2 = PlanItem.of(new TestElement());
    parent.addChild(child1);
    parent.addChild(child2);
    parent.activate();
    child1.activate();
    parent.terminate();
    assertThat(parent.getStatus()).isEqualTo(PlanItemStatus.TERMINATED);
    assertThat(child1.getStatus()).isEqualTo(PlanItemStatus.TERMINATED);
    assertThat(child2.getStatus()).isEqualTo(PlanItemStatus.TERMINATED);
  }

  @Test
  void elementIsRetained() {
    TestElement element = new TestElement();
    PlanItem<TestElement> item = PlanItem.of(element);
    assertThat(item.getElement()).isSameAs(element);
  }
}
