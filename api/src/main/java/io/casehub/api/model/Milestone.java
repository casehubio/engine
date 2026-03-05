package io.casehub.api.model;

import java.util.Objects;

public class Milestone {

  private final String name;
  private final String condition;
  private String description;

  private Milestone(String name, String condition) {
    this.name = name;
    this.condition = condition;
  }

  public String getName() {
    return name;
  }

  public String getCondition() {
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
