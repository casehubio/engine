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
package io.casehub.blackboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Worker;
import io.casehub.blackboard.plan.CasePlanModel;
import io.casehub.blackboard.plan.CasePlanModelRegistry;
import io.casehub.blackboard.stage.Stage;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class NestedStageIntegrationTest {

  @Inject TwoSequentialStagesCaseHub sequentialBean;
  @Inject NestedStageCaseHub nestedBean;
  @Inject LambdaConditionCaseHub lambdaBean;
  @Inject CaseInstanceCache caseInstanceCache;

  @BeforeEach
  void reset() {
    TwoSequentialStagesCaseHub.stage1Ran.set(0);
    TwoSequentialStagesCaseHub.stage2Ran.set(0);
    NestedStageCaseHub.outerRan.set(false);
    NestedStageCaseHub.innerRan.set(false);
    LambdaConditionCaseHub.workerRan.set(false);
  }

  /** Test 1 — Two sequential stages: stage1 fires on phase=one, stage2 fires on phase=two. */
  @Test
  void twoSequentialStagesActivateInOrder() {
    AtomicReference<UUID> ref = new AtomicReference<>();
    AtomicReference<Throwable> err = new AtomicReference<>();

    sequentialBean
        .startCase(Map.of("phase", "one"))
        .thenAccept(ref::set)
        .exceptionally(
            ex -> {
              err.set(ex);
              return null;
            });

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              if (err.get() != null) throw new AssertionError(err.get());
              assertThat(ref.get()).isNotNull();
            });

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(ref.get());
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(TwoSequentialStagesCaseHub.stage1Ran.get()).isGreaterThan(0);
              assertThat(TwoSequentialStagesCaseHub.stage2Ran.get()).isGreaterThan(0);
            });
  }

  /**
   * Test 2 — Nested stage: outer stage fires, outerWorker enables inner stage, innerWorker runs.
   */
  @Test
  void nestedStageActivatesAfterOuterWorker() {
    AtomicReference<UUID> ref = new AtomicReference<>();
    AtomicReference<Throwable> err = new AtomicReference<>();

    nestedBean
        .startCase(Map.of("status", "outer-ready"))
        .thenAccept(ref::set)
        .exceptionally(
            ex -> {
              err.set(ex);
              return null;
            });

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              if (err.get() != null) throw new AssertionError(err.get());
              assertThat(ref.get()).isNotNull();
            });

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(ref.get());
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(NestedStageCaseHub.outerRan.get()).isTrue();
              assertThat(NestedStageCaseHub.innerRan.get()).isTrue();
            });
  }

  /** Test 3 — Lambda entry condition: stage fires when value == 42. */
  @Test
  void lambdaEntryConditionActivatesStage() {
    AtomicReference<UUID> ref = new AtomicReference<>();
    AtomicReference<Throwable> err = new AtomicReference<>();

    lambdaBean
        .startCase(Map.of("value", 42))
        .thenAccept(ref::set)
        .exceptionally(
            ex -> {
              err.set(ex);
              return null;
            });

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              if (err.get() != null) throw new AssertionError(err.get());
              assertThat(ref.get()).isNotNull();
            });

    await()
        .atMost(20, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              var instance = caseInstanceCache.get(ref.get());
              assertThat(instance).isNotNull();
              assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(LambdaConditionCaseHub.workerRan.get()).isTrue();
            });
  }

  // ---------------------------------------------------------------------------
  // Bean: TwoSequentialStagesCaseHub
  // ---------------------------------------------------------------------------

  @ApplicationScoped
  static class TwoSequentialStagesCaseHub extends CaseHub {

    static final AtomicInteger stage1Ran = new AtomicInteger(0);
    static final AtomicInteger stage2Ran = new AtomicInteger(0);

    @Inject CasePlanModelRegistry planRegistry;

    private final CaseDefinition definition;

    public TwoSequentialStagesCaseHub() {
      Capability cap1 =
          Capability.builder()
              .name("seq-cap-stage1")
              .inputSchema("{ phase: .phase }")
              .outputSchema("{ phase: .phase }")
              .build();

      Capability cap2 =
          Capability.builder()
              .name("seq-cap-stage2")
              .inputSchema("{ phase: .phase }")
              .outputSchema("{ status: .status }")
              .build();

      Goal goal =
          Goal.builder()
              .name("seq-done-goal")
              .condition(".status == \"done\"")
              .kind(GoalKind.SUCCESS)
              .build();

      Worker worker1 =
          Worker.builder()
              .name("seq-worker-stage1")
              .capabilities(cap1)
              .function(
                  input -> {
                    stage1Ran.incrementAndGet();
                    return Map.of("phase", "two");
                  })
              .build();

      Worker worker2 =
          Worker.builder()
              .name("seq-worker-stage2")
              .capabilities(cap2)
              .function(
                  input -> {
                    stage2Ran.incrementAndGet();
                    return Map.of("status", "done");
                  })
              .build();

      definition =
          CaseDefinition.builder()
              .namespace("blackboard-nested-test")
              .name("two-sequential-stages")
              .version("1.0.0")
              .capabilities(cap1, cap2)
              .workers(worker1, worker2)
              .bindings(
                  Binding.builder()
                      .name("seq-trigger-stage1")
                      .capability(cap1)
                      .on(new ContextChangeTrigger(".phase == \"one\""))
                      .build(),
                  Binding.builder()
                      .name("seq-trigger-stage2")
                      .capability(cap2)
                      .on(new ContextChangeTrigger(".phase == \"two\""))
                      .build())
              .goals(goal)
              .completion(GoalExpression.allOf(goal))
              .build();
    }

    @PostConstruct
    void registerPlan() {
      Worker worker1 = definition.getWorkers().get(0);
      Worker worker2 = definition.getWorkers().get(1);

      CasePlanModel planModel =
          CasePlanModel.builder()
              .name("two-sequential-stages-plan")
              .stages(
                  Stage.builder()
                      .name("seq-stage-one")
                      .entry(".phase == \"one\"")
                      .workers(worker1)
                      .build(),
                  Stage.builder()
                      .name("seq-stage-two")
                      .entry(".phase == \"two\"")
                      .workers(worker2)
                      .build())
              .build();

      planRegistry.register(definition, planModel);
    }

    @Override
    public CaseDefinition getDefinition() {
      return definition;
    }
  }

  // ---------------------------------------------------------------------------
  // Bean: NestedStageCaseHub
  // ---------------------------------------------------------------------------

  @ApplicationScoped
  static class NestedStageCaseHub extends CaseHub {

    static final AtomicBoolean outerRan = new AtomicBoolean(false);
    static final AtomicBoolean innerRan = new AtomicBoolean(false);

    @Inject CasePlanModelRegistry planRegistry;

    private final CaseDefinition definition;

    public NestedStageCaseHub() {
      Capability outerCap =
          Capability.builder()
              .name("nested-cap-outer")
              .inputSchema("{ status: .status }")
              .outputSchema("{ innerReady: .innerReady }")
              .build();

      Capability innerCap =
          Capability.builder()
              .name("nested-cap-inner")
              .inputSchema("{ innerReady: .innerReady }")
              .outputSchema("{ status: .status }")
              .build();

      Goal goal =
          Goal.builder()
              .name("nested-done-goal")
              .condition(".status == \"nested-done\"")
              .kind(GoalKind.SUCCESS)
              .build();

      Worker outerWorker =
          Worker.builder()
              .name("nested-outer-worker")
              .capabilities(outerCap)
              .function(
                  input -> {
                    outerRan.set(true);
                    return Map.of("innerReady", true);
                  })
              .build();

      Worker innerWorker =
          Worker.builder()
              .name("nested-inner-worker")
              .capabilities(innerCap)
              .function(
                  input -> {
                    innerRan.set(true);
                    return Map.of("status", "nested-done");
                  })
              .build();

      definition =
          CaseDefinition.builder()
              .namespace("blackboard-nested-test")
              .name("nested-stage-case")
              .version("1.0.0")
              .capabilities(outerCap, innerCap)
              .workers(outerWorker, innerWorker)
              .bindings(
                  Binding.builder()
                      .name("nested-trigger-outer")
                      .capability(outerCap)
                      .on(new ContextChangeTrigger(".status == \"outer-ready\""))
                      .build(),
                  Binding.builder()
                      .name("nested-trigger-inner")
                      .capability(innerCap)
                      .on(new ContextChangeTrigger(".innerReady == true"))
                      .build())
              .goals(goal)
              .completion(GoalExpression.allOf(goal))
              .build();
    }

    @PostConstruct
    void registerPlan() {
      Worker outerWorker = definition.getWorkers().get(0);
      Worker innerWorker = definition.getWorkers().get(1);

      Stage innerStage =
          Stage.builder()
              .name("nested-inner-stage")
              .entry(".innerReady == true")
              .workers(innerWorker)
              .build();

      CasePlanModel planModel =
          CasePlanModel.builder()
              .name("nested-stage-plan")
              .stages(
                  Stage.builder()
                      .name("nested-outer-stage")
                      .entry(".status == \"outer-ready\"")
                      .workers(outerWorker)
                      .nestedStage(innerStage)
                      .build())
              .build();

      planRegistry.register(definition, planModel);
    }

    @Override
    public CaseDefinition getDefinition() {
      return definition;
    }
  }

  // ---------------------------------------------------------------------------
  // Bean: LambdaConditionCaseHub
  // ---------------------------------------------------------------------------

  @ApplicationScoped
  static class LambdaConditionCaseHub extends CaseHub {

    static final AtomicBoolean workerRan = new AtomicBoolean(false);

    @Inject CasePlanModelRegistry planRegistry;

    private final CaseDefinition definition;

    public LambdaConditionCaseHub() {
      Capability cap =
          Capability.builder()
              .name("lambda-cap")
              .inputSchema("{ value: .value }")
              .outputSchema("{ status: .status }")
              .build();

      Goal goal =
          Goal.builder()
              .name("lambda-done-goal")
              .condition(".status == \"lambda-done\"")
              .kind(GoalKind.SUCCESS)
              .build();

      Worker worker =
          Worker.builder()
              .name("lambda-worker")
              .capabilities(cap)
              .function(
                  input -> {
                    workerRan.set(true);
                    return Map.of("status", "lambda-done");
                  })
              .build();

      definition =
          CaseDefinition.builder()
              .namespace("blackboard-nested-test")
              .name("lambda-condition-case")
              .version("1.0.0")
              .capabilities(cap)
              .workers(worker)
              .bindings(
                  Binding.builder()
                      .name("lambda-trigger")
                      .capability(cap)
                      .on(new ContextChangeTrigger(".value == 42"))
                      .build())
              .goals(goal)
              .completion(GoalExpression.allOf(goal))
              .build();
    }

    @PostConstruct
    void registerPlan() {
      Worker worker = definition.getWorkers().get(0);

      CasePlanModel planModel =
          CasePlanModel.builder()
              .name("lambda-condition-plan")
              .stages(
                  Stage.builder()
                      .name("lambda-stage")
                      .entry(
                          (CaseContext ctx) -> {
                            Object val = ctx.getPath("value");
                            return val instanceof Number n && n.intValue() == 42;
                          })
                      .workers(worker)
                      .build())
              .build();

      planRegistry.register(definition, planModel);
    }

    @Override
    public CaseDefinition getDefinition() {
      return definition;
    }
  }
}
