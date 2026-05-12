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
package io.casehub.engine.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.AllOfGoalExpression;
import io.casehub.api.model.AnyOfGoalExpression;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link AllOfGoalExpression} and {@link AnyOfGoalExpression}.
 *
 * <p>These drive case completion logic: the engine calls {@code expression.test(achievedGoals)}.
 * Getting this wrong means cases that should complete don't, or cases that shouldn't complete do.
 */
@DisplayName("GoalExpression")
class GoalExpressionTest {

  private Goal goalA;
  private Goal goalB;
  private Goal goalC;

  @BeforeEach
  void setUp() {
    goalA = Goal.builder().name("goal-a").condition(".a == true").kind(GoalKind.SUCCESS).build();
    goalB = Goal.builder().name("goal-b").condition(".b == true").kind(GoalKind.SUCCESS).build();
    goalC = Goal.builder().name("goal-c").condition(".c == true").kind(GoalKind.FAILURE).build();
  }

  // ================================================================== //
  //  AllOfGoalExpression                                                //
  // ================================================================== //

  @Nested
  @DisplayName("AllOfGoalExpression")
  class AllOfTests {

    @Test
    @DisplayName("empty allOf requirement — vacuously true regardless of current goals")
    void emptyAllOf_alwaysTrue() {
      final var expr = new AllOfGoalExpression(List.of());
      assertTrue(expr.test(List.of()), "empty allOf should be vacuously true with no goals");
      assertTrue(expr.test(List.of(goalA)), "empty allOf should be vacuously true with any goals");
      assertTrue(
          expr.test(null), "empty allOf should be vacuously true even with null current goals");
    }

    @Test
    @DisplayName("null allOf collection — vacuously true")
    void nullAllOf_alwaysTrue() {
      final var expr = new AllOfGoalExpression(null);
      assertTrue(expr.test(List.of()));
      assertTrue(expr.test(null));
    }

    @Test
    @DisplayName("single goal in allOf — requires that goal to be present")
    void singleGoal_requiresPresence() {
      final var expr = GoalExpression.allOf(goalA);
      assertTrue(expr.test(List.of(goalA)), "goal present — should be true");
      assertFalse(expr.test(List.of(goalB)), "different goal present — should be false");
      assertFalse(expr.test(List.of()), "no goals — should be false");
    }

    @Test
    @DisplayName("all goals in allOf must be present — partial match is false")
    void twoGoals_allMustBePresent() {
      final var expr = GoalExpression.allOf(goalA, goalB);
      assertTrue(expr.test(List.of(goalA, goalB)), "both present — true");
      assertFalse(expr.test(List.of(goalA)), "only A present — false");
      assertFalse(expr.test(List.of(goalB)), "only B present — false");
      assertFalse(expr.test(List.of()), "none present — false");
    }

    @Test
    @DisplayName("superset of required goals — still true")
    void currentGoalsIsSupersetOfRequired_isTrue() {
      final var expr = GoalExpression.allOf(goalA);
      assertTrue(
          expr.test(List.of(goalA, goalB, goalC)),
          "having more goals than required is still a pass");
    }

    @Test
    @DisplayName("null current goals — false when allOf has entries")
    void nullCurrentGoals_nonEmptyAllOf_isFalse() {
      final var expr = GoalExpression.allOf(goalA);
      assertFalse(expr.test(null), "null current goals with non-empty requirement is false");
    }

    @Test
    @DisplayName("getGoals returns the configured collection")
    void getGoals_returnsConfiguredCollection() {
      final var goals = List.of(goalA, goalB);
      final var expr = new AllOfGoalExpression(goals);
      assertTrue(expr.getGoals().containsAll(goals));
    }

    @Test
    @DisplayName("GoalExpression.allOf(Collection) factory delegates correctly")
    void factoryWithCollection_delegatesCorrectly() {
      final var expr = GoalExpression.allOf(List.of(goalA, goalB));
      assertTrue(expr.test(List.of(goalA, goalB)));
      assertFalse(expr.test(List.of(goalA)));
    }
  }

  // ================================================================== //
  //  AnyOfGoalExpression                                                //
  // ================================================================== //

  @Nested
  @DisplayName("AnyOfGoalExpression")
  class AnyOfTests {

    @Test
    @DisplayName("empty anyOf requirement — vacuously true")
    void emptyAnyOf_alwaysTrue() {
      final var expr = new AnyOfGoalExpression(List.of());
      assertTrue(expr.test(List.of()), "empty anyOf should be vacuously true");
      assertTrue(expr.test(List.of(goalA)));
      assertTrue(expr.test(null));
    }

    @Test
    @DisplayName("null anyOf collection — vacuously true")
    void nullAnyOf_alwaysTrue() {
      final var expr = new AnyOfGoalExpression(null);
      assertTrue(expr.test(List.of()));
    }

    @Test
    @DisplayName("single goal in anyOf — true when that goal is present")
    void singleGoal_trueWhenPresent() {
      final var expr = GoalExpression.anyOf(goalA);
      assertTrue(expr.test(List.of(goalA)));
      assertFalse(expr.test(List.of(goalB)));
      assertFalse(expr.test(List.of()));
    }

    @Test
    @DisplayName("first of two goals present — anyOf is true")
    void firstGoalPresent_isTrue() {
      final var expr = GoalExpression.anyOf(goalA, goalB);
      assertTrue(expr.test(List.of(goalA)), "first goal satisfies anyOf");
    }

    @Test
    @DisplayName("second of two goals present — anyOf is true")
    void secondGoalPresent_isTrue() {
      final var expr = GoalExpression.anyOf(goalA, goalB);
      assertTrue(expr.test(List.of(goalB)), "second goal satisfies anyOf");
    }

    @Test
    @DisplayName("neither goal present — anyOf is false")
    void neitherGoalPresent_isFalse() {
      final var expr = GoalExpression.anyOf(goalA, goalB);
      assertFalse(expr.test(List.of(goalC)));
    }

    @Test
    @DisplayName("empty current goals — anyOf with required goals is false")
    void emptyCurrentGoals_isFalse() {
      final var expr = GoalExpression.anyOf(goalA, goalB);
      assertFalse(expr.test(List.of()));
    }

    @Test
    @DisplayName("null current goals — anyOf with required goals is false")
    void nullCurrentGoals_isFalse() {
      final var expr = GoalExpression.anyOf(goalA);
      assertFalse(expr.test(null));
    }

    @Test
    @DisplayName("all goals present — anyOf is true")
    void allGoalsPresent_isTrue() {
      final var expr = GoalExpression.anyOf(goalA, goalB);
      assertTrue(expr.test(List.of(goalA, goalB)));
    }

    @Test
    @DisplayName("getGoals returns the configured collection")
    void getGoals_returnsConfiguredCollection() {
      final var goals = List.of(goalA, goalC);
      final var expr = new AnyOfGoalExpression(goals);
      assertTrue(expr.getGoals().containsAll(goals));
    }
  }

  // ================================================================== //
  //  Goal equality — foundational to containsAll/contains checks        //
  // ================================================================== //

  @Nested
  @DisplayName("Goal equality semantics (AllOfGoalExpression.test relies on this)")
  class GoalEqualitySemantics {

    @Test
    @DisplayName("same Goal instance is equal to itself")
    void sameInstance_equal() {
      assertTrue(goalA.equals(goalA));
    }

    @Test
    @DisplayName("equal by fields — same name/condition/kind — should be equal")
    void equalByFields_equal() {
      final var goalA2 =
          Goal.builder().name("goal-a").condition(".a == true").kind(GoalKind.SUCCESS).build();
      assertTrue(goalA.equals(goalA2), "goals with same fields should be equal");
    }

    @Test
    @DisplayName("different name — not equal")
    void differentName_notEqual() {
      final var other =
          Goal.builder().name("goal-x").condition(".a == true").kind(GoalKind.SUCCESS).build();
      assertFalse(goalA.equals(other));
    }

    @Test
    @DisplayName("different kind — not equal")
    void differentKind_notEqual() {
      final var other =
          Goal.builder().name("goal-a").condition(".a == true").kind(GoalKind.FAILURE).build();
      assertFalse(goalA.equals(other));
    }

    @Test
    @DisplayName("allOf with equal Goal objects works correctly")
    void allOf_worksWithEqualGoalInstances() {
      // goalA2 is a NEW instance equal to goalA by fields
      final var goalA2 =
          Goal.builder().name("goal-a").condition(".a == true").kind(GoalKind.SUCCESS).build();

      final var expr = GoalExpression.allOf(goalA);

      // The achieved list holds goalA2 (a different instance, equal by value)
      final Collection<Goal> achieved = new ArrayList<>();
      achieved.add(goalA2);

      assertTrue(
          expr.test(achieved),
          "containsAll relies on equals — equal-by-field Goal instances must satisfy allOf");
    }
  }
}
