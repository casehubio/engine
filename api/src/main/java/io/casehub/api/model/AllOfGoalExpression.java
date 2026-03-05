package io.casehub.api.model;

import java.util.Collection;

public class AllOfGoalExpression implements GoalExpression {

  private final Collection<Goal> allOf;

  private AllOfGoalExpression(Collection<Goal> allOf) {
    this.allOf = allOf;
  }

  @Override
  public Collection<Goal> getGoals() {
    return allOf;
  }

  @Override
  public boolean test(Collection<Goal> currentGoals) {
    if (allOf == null || allOf.isEmpty()) {
      return true;
    }
    return currentGoals != null && currentGoals.containsAll(allOf);
  }
}
