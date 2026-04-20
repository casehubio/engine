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

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.api.model.evaluator.LambdaExpressionEvaluator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * CMMN Stage — a container for PlanItems, Milestones, and nested Stages that activates and
 * completes as a unit. Entry/exit conditions use {@link ExpressionEvaluator} (JQ or Lambda). See
 * casehubio/engine#76.
 *
 * <p>Milestone containment ({@code containedMilestoneIds}) is present but Stage exit criteria
 * referencing milestone achievement requires the full alignment in casehubio/engine#84. For PR4,
 * exit conditions use expression evaluation against {@code CaseContext} only.
 *
 * <p>See CMMN 1.1 §5.4.4. Full audit: casehubio/engine#84.
 */
public class Stage {

  private final String stageId;
  private final String name;
  private StageStatus status;
  private final Instant createdAt;
  private Instant activatedAt;
  private Instant completedAt;
  private String parentStageId; // null means root stage

  // Conditions — null means "no condition"
  private ExpressionEvaluator entryCondition;
  private ExpressionEvaluator exitCondition;

  // Containment
  private final List<String> containedPlanItemIds = new CopyOnWriteArrayList<>();
  private final List<String> containedMilestoneIds =
      new CopyOnWriteArrayList<>(); // casehubio/engine#84
  private final List<String> containedStageIds = new CopyOnWriteArrayList<>();
  private final List<String> requiredItemIds = new CopyOnWriteArrayList<>();

  // Behaviour
  private boolean autocomplete = true;
  private boolean manualActivation = false;

  private Stage(String name) {
    this.stageId = UUID.randomUUID().toString();
    this.name = name;
    this.status = StageStatus.PENDING;
    this.createdAt = Instant.now();
  }

  public static Stage create(String name) {
    return new Stage(name);
  }

  // Fluent builder-style configuration (called before adding to CasePlanModel)
  public Stage withEntryCondition(ExpressionEvaluator condition) {
    this.entryCondition = condition;
    return this;
  }

  /**
   * Convenience overload — wraps a Java lambda in {@link LambdaExpressionEvaluator}. Use this for
   * Java DSL stage definitions where type safety is preferred over JQ string expressions.
   */
  public Stage withEntryCondition(Predicate<CaseContext> predicate) {
    this.entryCondition = new LambdaExpressionEvaluator(predicate);
    return this;
  }

  public Stage withExitCondition(ExpressionEvaluator condition) {
    this.exitCondition = condition;
    return this;
  }

  /**
   * Convenience overload — wraps a Java lambda in {@link LambdaExpressionEvaluator}. Use this for
   * Java DSL stage definitions where type safety is preferred over JQ string expressions.
   */
  public Stage withExitCondition(Predicate<CaseContext> predicate) {
    this.exitCondition = new LambdaExpressionEvaluator(predicate);
    return this;
  }

  public Stage withManualActivation(boolean manual) {
    this.manualActivation = manual;
    return this;
  }

  public Stage withAutocomplete(boolean autocomplete) {
    this.autocomplete = autocomplete;
    return this;
  }

  public Stage withParentStage(String parentStageId) {
    this.parentStageId = parentStageId;
    return this;
  }

  // Lifecycle transitions
  public void activate() {
    if (status == StageStatus.PENDING) {
      status = StageStatus.ACTIVE;
      activatedAt = Instant.now();
    }
  }

  public void complete() {
    if (status == StageStatus.ACTIVE || status == StageStatus.SUSPENDED) {
      status = StageStatus.COMPLETED;
      completedAt = Instant.now();
    }
  }

  public void terminate() {
    if (status == StageStatus.ACTIVE || status == StageStatus.SUSPENDED) {
      status = StageStatus.TERMINATED;
      completedAt = Instant.now();
    }
  }

  public void suspend() {
    if (status == StageStatus.ACTIVE) status = StageStatus.SUSPENDED;
  }

  public void resume() {
    if (status == StageStatus.SUSPENDED) status = StageStatus.ACTIVE;
  }

  /**
   * Transitions this stage to FAULTED due to an unrecoverable error. No-op if the stage is already
   * in a terminal state (COMPLETED, TERMINATED, FAULTED). Unlike hardware faults which can
   * interrupt any state, this guard prevents overwriting a cleanly completed stage with a fault
   * that arrived out of order.
   */
  public void fault() {
    if (!isTerminal()) {
      status = StageStatus.FAULTED;
      completedAt = Instant.now();
    }
  }

  public boolean isTerminal() {
    return status == StageStatus.COMPLETED
        || status == StageStatus.TERMINATED
        || status == StageStatus.FAULTED;
  }

  public boolean isActive() {
    return status == StageStatus.ACTIVE;
  }

  // Containment mutations
  public void addPlanItem(String planItemId) {
    if (!containedPlanItemIds.contains(planItemId)) containedPlanItemIds.add(planItemId);
  }

  public void addMilestone(String milestoneName) {
    if (!containedMilestoneIds.contains(milestoneName)) containedMilestoneIds.add(milestoneName);
  }

  public void addNestedStage(String stageId) {
    if (!containedStageIds.contains(stageId)) containedStageIds.add(stageId);
  }

  public void addRequiredItem(String itemId) {
    if (!requiredItemIds.contains(itemId)) requiredItemIds.add(itemId);
  }

  // Getters
  public String getStageId() {
    return stageId;
  }

  public String getName() {
    return name;
  }

  public StageStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Optional<String> getParentStageId() {
    return Optional.ofNullable(parentStageId);
  }

  public ExpressionEvaluator getEntryCondition() {
    return entryCondition;
  }

  public ExpressionEvaluator getExitCondition() {
    return exitCondition;
  }

  public List<String> getContainedPlanItemIds() {
    return List.copyOf(containedPlanItemIds);
  }

  public List<String> getContainedMilestoneIds() {
    return List.copyOf(containedMilestoneIds);
  }

  public List<String> getContainedStageIds() {
    return List.copyOf(containedStageIds);
  }

  public List<String> getRequiredItemIds() {
    return List.copyOf(requiredItemIds);
  }

  public boolean isAutocomplete() {
    return autocomplete;
  }

  public boolean isManualActivation() {
    return manualActivation;
  }
}
