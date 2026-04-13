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

public class Goal {

  private final String name;
  private final ExpressionEvaluator condition;
  private final GoalKind kind;
  private boolean terminal;
  private String description;

  public Goal(String name, ExpressionEvaluator condition, GoalKind kind) {
    this.name = name;
    this.condition = condition;
    this.kind = kind;
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

  public GoalKind getKind() {
    return kind;
  }

  public Boolean getTerminal() {
    return terminal;
  }

  public void setTerminal(boolean terminal) {
    this.terminal = terminal;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private ExpressionEvaluator condition;
    private GoalKind kind;
    private boolean terminal;
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
      this.condition = new JQExpressionEvaluator(condition);
      return this;
    }

    public Builder kind(GoalKind kind) {
      this.kind = kind;
      return this;
    }

    public Builder terminal(boolean terminal) {
      this.terminal = terminal;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Goal build() {
      Goal goal =
          new Goal(
              Objects.requireNonNull(name),
              Objects.requireNonNull(condition),
              Objects.requireNonNull(kind));
      goal.setTerminal(terminal);
      goal.setDescription(description);
      return goal;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Goal goal)) return false;
    return Objects.equals(name, goal.name)
        && Objects.equals(condition, goal.condition)
        && kind == goal.kind
        && Objects.equals(terminal, goal.terminal)
        && Objects.equals(description, goal.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, condition, kind, terminal, description);
  }
}
