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
package io.casehub.blackboard.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.PlanExecutionContext;
import io.casehub.api.model.CaseDefinition;
import io.casehub.blackboard.event.BlackboardEventBusAddresses;
import io.casehub.blackboard.plan.DefaultCasePlanModel;
import io.casehub.blackboard.stage.Stage;
import io.casehub.blackboard.stage.StageStatus;
import io.vertx.mutiny.core.eventbus.EventBus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for StageLifecycleEvaluator entry/exit evaluation. See casehubio/engine#76. */
class StageLifecycleEvaluatorTest {

  private StageLifecycleEvaluator evaluator;
  private EventBus mockBus;
  private DefaultCasePlanModel plan;
  private PlanExecutionContext ctx;

  @BeforeEach
  void setUp() {
    mockBus = mock(EventBus.class);
    evaluator = new StageLifecycleEvaluator(mockBus);
    UUID caseId = UUID.randomUUID();
    plan = new DefaultCasePlanModel(caseId);
    CaseContext mockCtx = mock(CaseContext.class);
    ctx = new PlanExecutionContext(caseId, mock(CaseDefinition.class), mockCtx);
  }

  @Test
  void pending_stage_activates_when_entry_condition_met() {
    Stage stage = Stage.create("intake").withEntryCondition(c -> true);
    plan.addStage(stage);

    evaluator.evaluate(plan, ctx).await().indefinitely();

    assertThat(stage.getStatus()).isEqualTo(StageStatus.ACTIVE);
    verify(mockBus).publish(eq(BlackboardEventBusAddresses.STAGE_ACTIVATED), any());
  }

  @Test
  void pending_stage_stays_pending_when_entry_condition_not_met() {
    Stage stage = Stage.create("intake").withEntryCondition(c -> false);
    plan.addStage(stage);

    evaluator.evaluate(plan, ctx).await().indefinitely();

    assertThat(stage.getStatus()).isEqualTo(StageStatus.PENDING);
    verifyNoInteractions(mockBus);
  }

  @Test
  void pending_stage_with_no_entry_condition_activates() {
    Stage stage = Stage.create("intake"); // no entry condition
    plan.addStage(stage);

    evaluator.evaluate(plan, ctx).await().indefinitely();

    assertThat(stage.getStatus()).isEqualTo(StageStatus.ACTIVE);
  }

  @Test
  void active_stage_terminates_when_exit_condition_met() {
    Stage stage = Stage.create("intake").withExitCondition(c -> true);
    stage.activate();
    plan.addStage(stage);

    evaluator.evaluate(plan, ctx).await().indefinitely();

    assertThat(stage.getStatus()).isEqualTo(StageStatus.TERMINATED);
    verify(mockBus).publish(eq(BlackboardEventBusAddresses.STAGE_TERMINATED), any());
  }

  @Test
  void active_stage_stays_active_when_exit_condition_not_met() {
    Stage stage = Stage.create("intake").withExitCondition(c -> false);
    stage.activate();
    plan.addStage(stage);

    evaluator.evaluate(plan, ctx).await().indefinitely();

    assertThat(stage.getStatus()).isEqualTo(StageStatus.ACTIVE);
    verifyNoInteractions(mockBus);
  }

  @Test
  void manual_activation_stage_stays_pending_even_when_entry_met() {
    Stage stage = Stage.create("intake").withEntryCondition(c -> true).withManualActivation(true);
    plan.addStage(stage);

    evaluator.evaluate(plan, ctx).await().indefinitely();

    assertThat(stage.getStatus()).isEqualTo(StageStatus.PENDING);
    verifyNoInteractions(mockBus);
  }
}
