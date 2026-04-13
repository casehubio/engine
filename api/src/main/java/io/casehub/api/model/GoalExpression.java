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
