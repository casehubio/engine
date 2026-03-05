package io.casehub.api.model;

import java.util.Collection;
import java.util.function.Predicate;

public interface GoalExpression extends Predicate<Collection<Goal>> {

  Collection<Goal> getGoals();
}
