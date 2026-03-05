package io.casehub.api.model;

import java.util.Collection;
import java.util.List;

public class AnyOfGoalExpression implements GoalExpression {

  private final List<Goal> anyOf;

  private AnyOfGoalExpression(List<Goal> anyOf) {
    this.anyOf = anyOf;
  }

  @Override
  public Collection<Goal> getGoals() {
    return anyOf;
  }

  @Override
  public boolean test(Collection<Goal> currentGoals) {
    if (anyOf == null || anyOf.isEmpty()) {
      return true;
    }
    if (currentGoals == null || currentGoals.isEmpty()) {
      return false;
    }
    for (Goal goal : anyOf) {
      if (currentGoals.contains(goal)) {
        return true;
      }
    }
    return false;
  }
}
