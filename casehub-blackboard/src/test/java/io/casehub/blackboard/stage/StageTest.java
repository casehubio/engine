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
package io.casehub.blackboard.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.model.evaluator.LambdaExpressionEvaluator;
import io.casehub.api.plan.PlanElement;
import org.junit.jupiter.api.Test;

class StageTest {

  private static Worker worker(String name) {
    return Worker.builder()
        .name(name)
        .capabilities(
            Capability.builder().name("cap-" + name).inputSchema("{}").outputSchema("{}").build())
        .function(input -> input)
        .build();
  }

  @Test
  void stageImplementsPlanElement() {
    Stage stage = Stage.builder().name("s").entry(".x == true").build();
    assertThat(stage).isInstanceOf(PlanElement.class);
  }

  @Test
  void nameIsRequired() {
    assertThatThrownBy(() -> Stage.builder().entry(".x == true").build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void entryConditionIsRequired() {
    assertThatThrownBy(() -> Stage.builder().name("s").build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void entryStringCreatesJQEvaluator() {
    Stage stage = Stage.builder().name("s").entry(".x == true").build();
    assertInstanceOf(JQExpressionEvaluator.class, stage.getEntryCondition());
  }

  @Test
  void entryLambdaCreatesLambdaEvaluator() {
    Stage stage = Stage.builder().name("s").entry(ctx -> true).build();
    assertInstanceOf(LambdaExpressionEvaluator.class, stage.getEntryCondition());
  }

  @Test
  void nullEntryStringThrows() {
    assertThatThrownBy(() -> Stage.builder().name("s").entry((String) null).build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void nullEntryLambdaThrows() {
    assertThatThrownBy(
            () ->
                Stage.builder()
                    .name("s")
                    .entry((java.util.function.Predicate<io.casehub.api.context.CaseContext>) null)
                    .build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void exitConditionIsOptional() {
    Stage stage = Stage.builder().name("s").entry(".x == true").build();
    assertThat(stage.getExitCondition()).isNull();
  }

  @Test
  void exitStringCreatesJQEvaluator() {
    Stage stage = Stage.builder().name("s").entry(".x == true").exit(".y == true").build();
    assertInstanceOf(JQExpressionEvaluator.class, stage.getExitCondition());
  }

  @Test
  void exitLambdaCreatesLambdaEvaluator() {
    Stage stage = Stage.builder().name("s").entry(".x == true").exit(ctx -> false).build();
    assertInstanceOf(LambdaExpressionEvaluator.class, stage.getExitCondition());
  }

  @Test
  void workersDefaultToEmpty() {
    Stage stage = Stage.builder().name("s").entry(".x == true").build();
    assertThat(stage.getWorkers()).isEmpty();
  }

  @Test
  void nestedStagesDefaultToEmpty() {
    Stage stage = Stage.builder().name("s").entry(".x == true").build();
    assertThat(stage.getNestedStages()).isEmpty();
  }

  @Test
  void workersAreRetained() {
    Worker w = worker("w1");
    Stage stage = Stage.builder().name("s").entry(".x == true").workers(w).build();
    assertThat(stage.getWorkers()).containsExactly(w);
  }

  @Test
  void nestedStageIsRetained() {
    Stage nested = Stage.builder().name("inner").entry(".y == true").build();
    Stage outer = Stage.builder().name("outer").entry(".x == true").nestedStage(nested).build();
    assertThat(outer.getNestedStages()).containsExactly(nested);
  }
}
