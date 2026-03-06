package io.casehub.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

public interface GoalExpression extends Predicate<Collection<Goal>> {

  Collection<Goal> getGoals();

  static GoalExpression allOf(Collection<Goal> goals) {
    return new AllOfGoalExpression(goals);
  }

  static GoalExpression allOf(Goal... goals) {
    return new AllOfGoalExpression(Arrays.asList(goals));
  }

  static GoalExpression anyOf(Goal... goals) {
    return new AnyOfGoalExpression(Arrays.asList(goals));
  }

}
