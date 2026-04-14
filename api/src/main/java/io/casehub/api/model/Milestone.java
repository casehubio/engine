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
package io.casehub.api.model;

import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import java.util.Objects;

/**
 * A named waypoint that a case passes through on its way to a {@link Goal}.
 *
 * <p>Milestones and goals answer different questions:
 *
 * <ul>
 *   <li><b>Milestones</b> — where are we? A milestone marks a point of progress. It has no
 *       success/failure polarity; it is a neutral checkpoint that the case either has or has not
 *       reached. You <em>pass</em> milestones.
 *   <li><b>Goals</b> — what outcome are we trying to achieve? A goal carries {@link GoalKind}
 *       (SUCCESS or FAILURE) and drives case completion. You <em>achieve</em> goals.
 * </ul>
 *
 * <p>Example — loan application case:
 *
 * <pre>{@code
 * // Milestones: intermediate waypoints
 * Milestone.builder().name("documents-received").condition(".docsUploaded == true").build()
 * Milestone.builder().name("credit-check-complete").condition(".creditScore != null").build()
 * Milestone.builder().name("underwriting-done").condition(".underwritingStatus == \"complete\"").build()
 *
 * // Goals: terminal outcomes
 * Goal.builder().name("loan-approved").condition(".decision == \"approved\"").kind(GoalKind.SUCCESS).build()
 * Goal.builder().name("loan-rejected").condition(".decision == \"rejected\"").kind(GoalKind.FAILURE).build()
 * }</pre>
 *
 * <h3>Lightweight use</h3>
 *
 * <p>By default, when the condition becomes true, a {@code MilestoneReachedEvent} is published on
 * the event bus and recorded in the {@link io.casehub.engine.internal.history.EventLog} as {@code
 * MILESTONE_REACHED}. No further tracking occurs. This is sufficient for observability, dashboards,
 * and audit trails.
 *
 * <h3>Lifecycle use (Phase 2 — with {@code CasePlanModel})</h3>
 *
 * <p>When a {@code CasePlanModel} is present (the CMMN/Blackboard layer), milestones are promoted
 * to lifecycle-tracked achievement markers with PENDING → ACHIEVED states. The {@code
 * MilestoneReachedEvent} triggers the PENDING → ACHIEVED transition, and the achieved state can be
 * referenced in stage exit criteria and case completion logic. The {@code Milestone} class itself
 * does not change — the lifecycle tracking is a {@code CasePlanModel} concern.
 */
public class Milestone {

  private final String name;
  private final ExpressionEvaluator condition;
  private String description;

  public Milestone(String name, ExpressionEvaluator condition) {
    this.name = name;
    this.condition = condition;
  }

  public String getName() {
    return name;
  }

  public ExpressionEvaluator getCondition() {
    return condition;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private ExpressionEvaluator condition;
    private String description;

    private Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder condition(ExpressionEvaluator condition) {
      this.condition = condition;
      return this;
    }

    public Builder condition(String condition) {
      this.condition =
          new JQExpressionEvaluator(
              Objects.requireNonNull(condition, "condition must not be null"));
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Milestone build() {
      Milestone milestone =
          new Milestone(Objects.requireNonNull(name), Objects.requireNonNull(condition));
      milestone.setDescription(description);
      return milestone;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Milestone milestone)) return false;
    return Objects.equals(name, milestone.name)
        && Objects.equals(condition, milestone.condition)
        && Objects.equals(description, milestone.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, condition, description);
  }
}
