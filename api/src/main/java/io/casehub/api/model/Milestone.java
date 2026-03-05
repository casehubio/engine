package io.casehub.api.model;

import io.casehub.api.model.evaluator.ExpressionEvaluator;

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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Milestone milestone)) return false;
    return Objects.equals(name, milestone.name) && Objects.equals(condition, milestone.condition) && Objects.equals(description, milestone.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, condition, description);
  }
}
