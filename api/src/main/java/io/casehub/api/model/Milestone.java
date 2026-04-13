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
      this.condition = new JQExpressionEvaluator(condition);
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
