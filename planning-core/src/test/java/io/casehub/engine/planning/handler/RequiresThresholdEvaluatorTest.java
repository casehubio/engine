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
package io.casehub.engine.planning.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.casehub.api.model.BindingRequires;
import io.casehub.api.model.ExecutorRef;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.engine.planning.plan.DefaultCasePlanModel;
import io.casehub.engine.planning.plan.PlanItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequiresThresholdEvaluatorTest {

  private UUID caseId;
  private String tenancyId;
  private DefaultCasePlanModel plan;
  private CompoundCompletionEvaluator compoundEvaluator;
  private List<PlanItemStateChangedEvent> firedEvents;
  private RequiresThresholdEvaluator evaluator;

  @BeforeEach
  void setUp() {
    caseId = UUID.randomUUID();
    tenancyId = "test-tenant";
    plan = new DefaultCasePlanModel(caseId);
    compoundEvaluator = mock(CompoundCompletionEvaluator.class);
    firedEvents = new ArrayList<>();
    Consumer<PlanItemStateChangedEvent> eventConsumer = firedEvents::add;
    evaluator = new RequiresThresholdEvaluator(compoundEvaluator, eventConsumer);
  }

  private PlanItem completedItem(String bindingName) {
    PlanItem pi = PlanItem.create(bindingName, ExecutorRef.of("worker"), 0);
    pi.markRunning();
    pi.markCompleted();
    return pi;
  }

  private PlanItem runningItem(String bindingName) {
    PlanItem pi = PlanItem.create(bindingName, ExecutorRef.of("worker"), 0);
    pi.markRunning();
    return pi;
  }

  private PlanItem faultedItem(String bindingName) {
    PlanItem pi = PlanItem.create(bindingName, ExecutorRef.of("worker"), 0);
    pi.markFaulted();
    return pi;
  }

  private PlanItemStateChangedEvent completionEvent(String bindingName, String planItemId) {
    return new PlanItemStateChangedEvent(
        caseId, planItemId, bindingName, TaskStatus.RUNNING, TaskStatus.COMPLETED, tenancyId);
  }

  private PlanItemStateChangedEvent faultEvent(String bindingName, String planItemId) {
    return new PlanItemStateChangedEvent(
        caseId, planItemId, bindingName, TaskStatus.RUNNING, TaskStatus.FAULTED, tenancyId);
  }

  @Test
  void cancelRemainingOnThresholdReached() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.CANCEL));

    PlanItem piA = completedItem("a");
    PlanItem piB = completedItem("b");
    PlanItem piC = runningItem("c");
    plan.addPlanItem(piA);
    plan.addPlanItem(piB);
    plan.addPlanItem(piC);

    evaluator.onPlanItemStateChanged(plan, completionEvent("b", piB.id()));

    assertThat(piC.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    assertThat(firedEvents)
        .anyMatch(e -> "c".equals(e.bindingName()) && e.newStatus() == TaskStatus.CANCELLED);
    verify(compoundEvaluator).evaluate(eq(caseId), eq(tenancyId), eq(plan), eq("c"));
  }

  @Test
  void keepRemainingOnThresholdReached() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.KEEP));

    PlanItem piA = completedItem("a");
    PlanItem piB = completedItem("b");
    PlanItem piC = runningItem("c");
    plan.addPlanItem(piA);
    plan.addPlanItem(piB);
    plan.addPlanItem(piC);

    evaluator.onPlanItemStateChanged(plan, completionEvent("b", piB.id()));

    assertThat(piC.getStatus()).isEqualTo(TaskStatus.RUNNING);
    assertThat(firedEvents).noneMatch(e -> "c".equals(e.bindingName()));
    verify(compoundEvaluator, never()).evaluate(any(), any(), any(), any());
  }

  @Test
  void faultOnUnreachableQuorum() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.KEEP));

    PlanItem piA = completedItem("a");
    PlanItem piB = faultedItem("b");
    PlanItem piC = faultedItem("c");
    plan.addPlanItem(piA);
    plan.addPlanItem(piB);
    plan.addPlanItem(piC);

    evaluator.onPlanItemStateChanged(plan, faultEvent("c", piC.id()));

    var aggItem = plan.findPlanItemByBindingName("aggregate");
    assertThat(aggItem).isPresent();
    assertThat(aggItem.get().getStatus()).isEqualTo(TaskStatus.FAULTED);
    assertThat(firedEvents)
        .anyMatch(e -> "aggregate".equals(e.bindingName()) && e.newStatus() == TaskStatus.FAULTED);
    verify(compoundEvaluator).evaluate(eq(caseId), eq(tenancyId), eq(plan), eq("aggregate"));
  }

  @Test
  void noFaultWhenQuorumStillReachable() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.KEEP));

    PlanItem piA = completedItem("a");
    PlanItem piB = faultedItem("b");
    PlanItem piC = runningItem("c");
    plan.addPlanItem(piA);
    plan.addPlanItem(piB);
    plan.addPlanItem(piC);

    evaluator.onPlanItemStateChanged(plan, faultEvent("b", piB.id()));

    var aggItem = plan.findPlanItemByBindingName("aggregate");
    assertThat(aggItem).isEmpty();
    assertThat(firedEvents).noneMatch(e -> "aggregate".equals(e.bindingName()));
    verify(compoundEvaluator, never()).evaluate(any(), any(), any(), any());
  }

  @Test
  void thresholdTriggeredOnlyOnce() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.CANCEL));

    PlanItem piA = completedItem("a");
    PlanItem piB = completedItem("b");
    PlanItem piC = runningItem("c");
    plan.addPlanItem(piA);
    plan.addPlanItem(piB);
    plan.addPlanItem(piC);

    evaluator.onPlanItemStateChanged(plan, completionEvent("b", piB.id()));

    assertThat(piC.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    long cancelEventCount =
        firedEvents.stream()
            .filter(e -> "c".equals(e.bindingName()) && e.newStatus() == TaskStatus.CANCELLED)
            .count();
    assertThat(cancelEventCount).isEqualTo(1);

    firedEvents.clear();

    PlanItem piC2 = completedItem("c");
    plan.addPlanItem(piC2);
    evaluator.onPlanItemStateChanged(plan, completionEvent("c", piC2.id()));

    assertThat(firedEvents).noneMatch(e -> e.newStatus() == TaskStatus.CANCELLED);
  }

  @Test
  void noActionWhenCompletedBindingNotInAnyRequires() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b"), 2, OnThresholdReached.CANCEL));

    PlanItem piX = completedItem("x");
    plan.addPlanItem(piX);

    evaluator.onPlanItemStateChanged(plan, completionEvent("x", piX.id()));

    assertThat(firedEvents).isEmpty();
    verify(compoundEvaluator, never()).evaluate(any(), any(), any(), any());
  }

  private void drainCascade() {
    int guard = 0;
    while (!firedEvents.isEmpty() && guard++ < 20) {
      var pending = new ArrayList<>(firedEvents);
      firedEvents.clear();
      for (var event : pending) {
        evaluator.onPlanItemStateChanged(plan, event);
      }
    }
  }

  @Test
  void nestedRequiresFaultCascadesThroughTwoLevels() {
    plan.registerRequiresMetadata(
        "enrich-counterparty",
        new BindingRequires(List.of("check-kyc", "check-sanctions"), 2, OnThresholdReached.KEEP));
    plan.registerRequiresMetadata(
        "settle-trade",
        new BindingRequires(
            List.of("enrich-counterparty", "enrich-instrument", "enrich-market-data"),
            2,
            OnThresholdReached.CANCEL));

    PlanItem kyc = faultedItem("check-kyc");
    PlanItem sanctions = faultedItem("check-sanctions");
    PlanItem instrument = completedItem("enrich-instrument");
    PlanItem marketData = runningItem("enrich-market-data");
    plan.addPlanItem(kyc);
    plan.addPlanItem(sanctions);
    plan.addPlanItem(instrument);
    plan.addPlanItem(marketData);

    evaluator.onPlanItemStateChanged(plan, faultEvent("check-sanctions", sanctions.id()));
    drainCascade();

    var counterparty = plan.findPlanItemByBindingName("enrich-counterparty");
    assertThat(counterparty).isPresent();
    assertThat(counterparty.get().getStatus()).isEqualTo(TaskStatus.FAULTED);

    var settle = plan.findPlanItemByBindingName("settle-trade");
    assertThat(settle).isEmpty();
  }

  @Test
  void nestedRequiresFaultCascadesFullyWhenOuterAlsoUnreachable() {
    plan.registerRequiresMetadata(
        "enrich-counterparty",
        new BindingRequires(List.of("check-kyc", "check-sanctions"), 2, OnThresholdReached.KEEP));
    plan.registerRequiresMetadata(
        "settle-trade",
        new BindingRequires(
            List.of("enrich-counterparty", "enrich-instrument", "enrich-market-data"),
            2,
            OnThresholdReached.CANCEL));

    PlanItem kyc = faultedItem("check-kyc");
    PlanItem sanctions = faultedItem("check-sanctions");
    PlanItem instrument = completedItem("enrich-instrument");
    PlanItem marketData = faultedItem("enrich-market-data");
    plan.addPlanItem(kyc);
    plan.addPlanItem(sanctions);
    plan.addPlanItem(instrument);
    plan.addPlanItem(marketData);

    evaluator.onPlanItemStateChanged(plan, faultEvent("check-sanctions", sanctions.id()));
    drainCascade();

    var counterparty = plan.findPlanItemByBindingName("enrich-counterparty");
    assertThat(counterparty).isPresent();
    assertThat(counterparty.get().getStatus()).isEqualTo(TaskStatus.FAULTED);

    var settle = plan.findPlanItemByBindingName("settle-trade");
    assertThat(settle).isPresent();
    assertThat(settle.get().getStatus()).isEqualTo(TaskStatus.FAULTED);
  }

  @Test
  void nestedRequiresHappyPathWithCancel() {
    plan.registerRequiresMetadata(
        "enrich-counterparty",
        new BindingRequires(List.of("check-kyc", "check-sanctions"), 2, OnThresholdReached.KEEP));
    plan.registerRequiresMetadata(
        "settle-trade",
        new BindingRequires(
            List.of("enrich-counterparty", "enrich-instrument", "enrich-market-data"),
            2,
            OnThresholdReached.CANCEL));

    PlanItem kyc = completedItem("check-kyc");
    PlanItem sanctions = completedItem("check-sanctions");
    PlanItem counterparty = completedItem("enrich-counterparty");
    PlanItem instrument = completedItem("enrich-instrument");
    PlanItem marketData = runningItem("enrich-market-data");
    plan.addPlanItem(kyc);
    plan.addPlanItem(sanctions);
    plan.addPlanItem(counterparty);
    plan.addPlanItem(instrument);
    plan.addPlanItem(marketData);

    evaluator.onPlanItemStateChanged(plan, completionEvent("enrich-instrument", instrument.id()));
    drainCascade();

    assertThat(marketData.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    assertThat(plan.isRequiresThresholdTriggered("settle-trade")).isTrue();
  }

  @Test
  void cancelSkipsAlreadyTerminalItems() {
    plan.registerRequiresMetadata(
        "aggregate", new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.CANCEL));

    PlanItem piA = completedItem("a");
    PlanItem piB = completedItem("b");
    PlanItem piC = completedItem("c");
    plan.addPlanItem(piA);
    plan.addPlanItem(piB);
    plan.addPlanItem(piC);

    evaluator.onPlanItemStateChanged(plan, completionEvent("b", piB.id()));

    assertThat(firedEvents).noneMatch(e -> e.newStatus() == TaskStatus.CANCELLED);
  }
}
