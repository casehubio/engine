package io.casehub.api.model;

import java.util.Objects;

public class Goal {

  private final String name;
  private final String condition;
  private final GoalKind kind;
  private final Boolean terminal;
  private String description;

  private Goal(String name, String condition, GoalKind kind, Boolean terminal) {
    this.name = name;
    this.condition = condition;
    this.kind = kind;
    this.terminal = terminal;
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

  public GoalKind getKind() {
    return kind;
  }

  public Boolean getTerminal() {
    return terminal;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Goal goal)) return false;
    return Objects.equals(name, goal.name) && Objects.equals(condition, goal.condition) && kind == goal.kind && Objects.equals(terminal, goal.terminal) && Objects.equals(description, goal.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, condition, kind, terminal, description);
  }
}
