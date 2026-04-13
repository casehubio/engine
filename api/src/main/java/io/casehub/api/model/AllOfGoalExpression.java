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

import java.util.Collection;

public class AllOfGoalExpression implements GoalExpression {

  private final Collection<Goal> allOf;

  public AllOfGoalExpression(Collection<Goal> allOf) {
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
