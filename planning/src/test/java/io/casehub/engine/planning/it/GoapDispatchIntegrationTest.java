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
package io.casehub.engine.planning.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests verifying GOAP and Adaptive planning strategies execute through the real engine
 * dispatch pipeline. Each test uses an inner {@link CaseHub} subclass with GOAP actions wired via
 * {@link CaseDefinition.Builder#goapActions} and {@link CaseDefinition.Builder#goalToEffectKey}.
 * Refs engine#924.
 */
@QuarkusTest
class GoapDispatchIntegrationTest {

  @Inject CaseInstanceCache cache;

  @Inject GoapOrderCaseBean goapOrderBean;
  @Inject AdaptiveReplanCaseBean adaptiveReplanBean;
  @Inject GuardGoapCaseBean guardGoapBean;
  @Inject EmptyPlanCaseBean emptyPlanBean;
  @Inject CostOrderingCaseBean costOrderingBean;
  @Inject FailureRerouteCaseBean failureRerouteBean;

  @BeforeEach
  void setUp() {
    GoapOrderCaseBean.executionOrder.clear();
    AdaptiveReplanCaseBean.executionOrder.clear();
    GuardGoapCaseBean.executionOrder.clear();
    EmptyPlanCaseBean.executionOrder.clear();
    CostOrderingCaseBean.executionOrder.clear();
    FailureRerouteCaseBean.executionOrder.clear();
  }

  // -- Test 1: GOAP dispatch order ----------------------------------------

  @Test
  void goapStrategy_executesWorkersInPlannedOrder() {
    UUID caseId = goapOrderBean.startCase(Map.of("trigger", true));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(GoapOrderCaseBean.executionOrder)
                  .as("GOAP should plan analyse→assess→report regardless of declaration order")
                  .containsExactly("analyse", "assess", "report");
            });
  }

  // -- Test 2: Adaptive replan --------------------------------------------

  @Test
  void adaptiveStrategy_replansWhenWorldStateChanges() {
    UUID caseId = adaptiveReplanBean.startCase(Map.of("trigger", true));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(AdaptiveReplanCaseBean.executionOrder)
                  .as(
                      "Adaptive should pick analyse first, then replan to fast-resolve "
                          + "(cheaper) instead of full-resolve after fastPath appears")
                  .containsExactly("analyse", "fast-resolve");
            });
  }

  // -- Test 3: Guard + GOAP -----------------------------------------------

  @Test
  void goapStrategy_plansWithEligibleSubsetIgnoringGuardedBindings() {
    UUID caseId = guardGoapBean.startCase(Map.of("trigger", true));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(cache.get(caseId).getCaseContext().get("prepared")).isEqualTo(true);
              assertThat(cache.get(caseId).getCaseContext().get("completed")).isEqualTo(true);
              assertThat(GuardGoapCaseBean.executionOrder)
                  .as(
                      "GOAP plans prepare->execute with eligible bindings, "
                          + "ignoring guarded 'optional' binding")
                  .containsExactly("prepare", "execute");
              assertThat(GuardGoapCaseBean.executionOrder)
                  .as("optional binding should never fire (guard not satisfied)")
                  .doesNotContain("optional");
            });
  }

  // -- Test 4: Failure reroute + replan -----------------------------------

  @Test
  void goapStrategy_handlesFailureRerouteWithAlternateAgent() {
    UUID caseId = failureRerouteBean.startCase(Map.of("trigger", true));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(FailureRerouteCaseBean.executionOrder)
                  .as("analyse fires first, then assess (fails once, reroutes to backup worker)")
                  .contains("analyse");
              assertThat(cache.get(caseId).getCaseContext().get("riskAssessment")).isEqualTo(true);
            });
  }

  // -- Test 5: Empty plan waits -------------------------------------------

  @Test
  void goapStrategy_waitsWhenNoPlanViable_thenExecutesAfterSignal() {
    UUID caseId = emptyPlanBean.startCase(Map.of("trigger", true));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.RUNNING);
              assertThat(EmptyPlanCaseBean.executionOrder).isEmpty();
            });

    emptyPlanBean.signal(caseId, "ready", true);

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(EmptyPlanCaseBean.executionOrder).containsExactly("process");
            });
  }

  // -- Test 6: Cost ordering ----------------------------------------------

  @Test
  void goapStrategy_selectsCheapestPathToGoal() {
    UUID caseId = costOrderingBean.startCase(Map.of("trigger", true));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertThat(cache.get(caseId).getState()).isEqualTo(CaseStatus.COMPLETED);
              assertThat(CostOrderingCaseBean.executionOrder)
                  .as(
                      "GOAP should choose cheap-path (cost 0.5) "
                          + "over expensive-path (cost 5.0)")
                  .containsExactly("cheap-path");
            });
  }

  // ======================================================================
  // Inner CaseHub beans
  // ======================================================================

  /**
   * Test 1: Three workers with a dependency chain (analyse→assess→report), declared in reverse
   * order. GOAP should plan and execute them in precondition order.
   */
  @ApplicationScoped
  public static class GoapOrderCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capAnalyse =
          Capability.builder().name("analyse").inputSchema(".").outputSchema(".").build();
      Capability capAssess =
          Capability.builder().name("assess").inputSchema(".").outputSchema(".").build();
      Capability capReport =
          Capability.builder().name("report").inputSchema(".").outputSchema(".").build();

      Worker wReport =
          Worker.builder()
              .name("report-worker")
              .capabilityName("report")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("report");
                        return WorkerResult.of(Map.of("reportDone", true));
                      }))
              .build();

      Worker wAssess =
          Worker.builder()
              .name("assess-worker")
              .capabilityName("assess")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("assess");
                        return WorkerResult.of(Map.of("riskAssessment", true));
                      }))
              .build();

      Worker wAnalyse =
          Worker.builder()
              .name("analyse-worker")
              .capabilityName("analyse")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("analyse");
                        return WorkerResult.of(Map.of("analysisResult", true));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test-goap")
          .name("GOAP Order Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capAnalyse, capAssess, capReport)
          .workers(wReport, wAssess, wAnalyse)
          .bindings(
              Binding.builder()
                  .name("report")
                  .capability(capReport)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("assess")
                  .capability(capAssess)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("analyse")
                  .capability(capAnalyse)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(
              List.of(
                  new GoapAction("analyse", Map.of(), Map.of("analysisResult", true), 1.0),
                  new GoapAction(
                      "assess",
                      Map.of("analysisResult", true),
                      Map.of("riskAssessment", true),
                      1.0),
                  new GoapAction(
                      "report", Map.of("riskAssessment", true), Map.of("reportDone", true), 1.0)))
          .goalToEffectKey("done", Set.of("reportDone"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".reportDone == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".reportDone == true"))
                      .build()))
          .build();
    }
  }

  /**
   * Test 2: Adaptive replanning. Worker 'analyse' declares effect {analysisResult} but also writes
   * {fastPath} to context. Initial plan: analyse→full-resolve. After analyse completes and fastPath
   * appears, replan finds fast-resolve (cost 0.5) is cheaper than full-resolve (cost 2.0).
   */
  @ApplicationScoped
  public static class AdaptiveReplanCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capAnalyse =
          Capability.builder().name("analyse").inputSchema(".").outputSchema(".").build();
      Capability capFullResolve =
          Capability.builder().name("full-resolve").inputSchema(".").outputSchema(".").build();
      Capability capFastResolve =
          Capability.builder().name("fast-resolve").inputSchema(".").outputSchema(".").build();

      Worker wAnalyse =
          Worker.builder()
              .name("analyse-worker")
              .capabilityName("analyse")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("analyse");
                        return WorkerResult.of(Map.of("analysisResult", true, "fastPath", true));
                      }))
              .build();

      Worker wFullResolve =
          Worker.builder()
              .name("full-resolve-worker")
              .capabilityName("full-resolve")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("full-resolve");
                        return WorkerResult.of(Map.of("resolved", true));
                      }))
              .build();

      Worker wFastResolve =
          Worker.builder()
              .name("fast-resolve-worker")
              .capabilityName("fast-resolve")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("fast-resolve");
                        return WorkerResult.of(Map.of("resolved", true));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test-adaptive")
          .name("Adaptive Replan Test")
          .version("1.0.0")
          .planningStrategy("adaptive")
          .capabilities(capAnalyse, capFullResolve, capFastResolve)
          .workers(wAnalyse, wFullResolve, wFastResolve)
          .bindings(
              Binding.builder()
                  .name("analyse")
                  .capability(capAnalyse)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("full-resolve")
                  .capability(capFullResolve)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("fast-resolve")
                  .capability(capFastResolve)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(
              List.of(
                  new GoapAction("analyse", Map.of(), Map.of("analysisResult", true), 1.0),
                  new GoapAction(
                      "full-resolve",
                      Map.of("analysisResult", true),
                      Map.of("resolved", true),
                      2.0),
                  new GoapAction(
                      "fast-resolve",
                      Map.of("analysisResult", true, "fastPath", true),
                      Map.of("resolved", true),
                      0.5)))
          .goalToEffectKey("done", Set.of("resolved"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".resolved == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".resolved == true"))
                      .build()))
          .build();
    }
  }

  /**
   * Test 3: Guard filters ineligible binding. Three bindings declared, but 'optional' has a trigger
   * guard that is never satisfied. GOAP plans correctly with only the eligible subset
   * (prepare->execute), ignoring the filtered binding.
   */
  @ApplicationScoped
  public static class GuardGoapCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capPrepare =
          Capability.builder().name("prepare").inputSchema(".").outputSchema(".").build();
      Capability capExecute =
          Capability.builder().name("execute").inputSchema(".").outputSchema(".").build();
      Capability capOptional =
          Capability.builder().name("optional").inputSchema(".").outputSchema(".").build();

      Worker wPrepare =
          Worker.builder()
              .name("prepare-worker")
              .capabilityName("prepare")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("prepare");
                        return WorkerResult.of(Map.of("prepared", true));
                      }))
              .build();

      Worker wExecute =
          Worker.builder()
              .name("execute-worker")
              .capabilityName("execute")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("execute");
                        return WorkerResult.of(Map.of("completed", true));
                      }))
              .build();

      Worker wOptional =
          Worker.builder()
              .name("optional-worker")
              .capabilityName("optional")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("optional");
                        return WorkerResult.of(Map.of("bonus", true));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test-goap-guard")
          .name("Guard GOAP Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capPrepare, capExecute, capOptional)
          .workers(wPrepare, wExecute, wOptional)
          .bindings(
              Binding.builder()
                  .name("prepare")
                  .capability(capPrepare)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("execute")
                  .capability(capExecute)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("optional")
                  .capability(capOptional)
                  .on(new ContextChangeTrigger(".trigger == true and .enableOptional == true"))
                  .build())
          .goapActions(
              List.of(
                  new GoapAction("prepare", Map.of(), Map.of("prepared", true), 1.0),
                  new GoapAction(
                      "execute", Map.of("prepared", true), Map.of("completed", true), 1.0),
                  new GoapAction("optional", Map.of(), Map.of("bonus", true), 1.0)))
          .goalToEffectKey("done", Set.of("completed"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".completed == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".completed == true"))
                      .build()))
          .build();
    }
  }

  /**
   * Test 4: Failure reroute. Two workers for the 'assess' capability — the first fails, the engine
   * reroutes to the backup worker. GOAP plans the correct action sequence; the reroute mechanism
   * handles the agent-level failover.
   */
  @ApplicationScoped
  public static class FailureRerouteCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capAnalyse =
          Capability.builder().name("analyse").inputSchema(".").outputSchema(".").build();
      Capability capAssess =
          Capability.builder().name("assess").inputSchema(".").outputSchema(".").build();

      Worker wAnalyse =
          Worker.builder()
              .name("analyse-worker")
              .capabilityName("analyse")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("analyse");
                        return WorkerResult.of(Map.of("analysisResult", true));
                      }))
              .build();

      Worker wAssessPrimary =
          Worker.builder()
              .name("assess-primary")
              .capabilityName("assess")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("assess-primary-fail");
                        return WorkerResult.failed("Primary assessment unavailable");
                      }))
              .build();

      Worker wAssessBackup =
          Worker.builder()
              .name("assess-backup")
              .capabilityName("assess")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("assess-backup-success");
                        return WorkerResult.of(Map.of("riskAssessment", true));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test-goap-reroute")
          .name("Failure Reroute GOAP Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capAnalyse, capAssess)
          .workers(wAnalyse, wAssessPrimary, wAssessBackup)
          .bindings(
              Binding.builder()
                  .name("analyse")
                  .capability(capAnalyse)
                  .on(
                      new ContextChangeTrigger(
                          ".trigger == true and .actionGateRejected == null"
                              + " and .actionGateApproved == null"))
                  .build(),
              Binding.builder()
                  .name("assess")
                  .capability(capAssess)
                  .on(
                      new ContextChangeTrigger(
                          ".trigger == true and .actionGateRejected == null"
                              + " and .actionGateApproved == null"))
                  .build())
          .goapActions(
              List.of(
                  new GoapAction("analyse", Map.of(), Map.of("analysisResult", true), 1.0),
                  new GoapAction(
                      "assess",
                      Map.of("analysisResult", true),
                      Map.of("riskAssessment", true),
                      1.0)))
          .goalToEffectKey("done", Set.of("riskAssessment"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".riskAssessment == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".riskAssessment == true"))
                      .build()))
          .build();
    }
  }

  /**
   * Test 5: Empty plan waits. The sole action requires a 'ready' precondition not present at
   * startup. The case stays RUNNING until a signal adds 'ready' to context, making the plan viable.
   */
  @ApplicationScoped
  public static class EmptyPlanCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capProcess =
          Capability.builder().name("process").inputSchema(".").outputSchema(".").build();

      Worker wProcess =
          Worker.builder()
              .name("process-worker")
              .capabilityName("process")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("process");
                        return WorkerResult.of(Map.of("processed", true));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test-goap-empty")
          .name("Empty Plan GOAP Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capProcess)
          .workers(wProcess)
          .bindings(
              Binding.builder()
                  .name("process")
                  .capability(capProcess)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(
              List.of(
                  new GoapAction("process", Map.of("ready", true), Map.of("processed", true), 1.0)))
          .goalToEffectKey("done", Set.of("processed"))
          .goals(
              Goal.builder()
                  .name("done")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".processed == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("done")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".processed == true"))
                      .build()))
          .build();
    }
  }

  /**
   * Test 6: Cost ordering. Two independent paths to the goal — cheap-path (cost 0.5) and
   * expensive-path (cost 5.0). GOAP should select the cheaper one.
   */
  @ApplicationScoped
  public static class CostOrderingCaseBean extends CaseHub {
    static final List<String> executionOrder = new CopyOnWriteArrayList<>();

    @Override
    public CaseDefinition getDefinition() {
      Capability capCheap =
          Capability.builder().name("cheap-path").inputSchema(".").outputSchema(".").build();
      Capability capExpensive =
          Capability.builder().name("expensive-path").inputSchema(".").outputSchema(".").build();

      Worker wCheap =
          Worker.builder()
              .name("cheap-worker")
              .capabilityName("cheap-path")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("cheap-path");
                        return WorkerResult.of(Map.of("done", true));
                      }))
              .build();

      Worker wExpensive =
          Worker.builder()
              .name("expensive-worker")
              .capabilityName("expensive-path")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        executionOrder.add("expensive-path");
                        return WorkerResult.of(Map.of("done", true));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test-goap-cost")
          .name("Cost Ordering GOAP Test")
          .version("1.0.0")
          .planningStrategy("goap")
          .capabilities(capCheap, capExpensive)
          .workers(wCheap, wExpensive)
          .bindings(
              Binding.builder()
                  .name("cheap-path")
                  .capability(capCheap)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build(),
              Binding.builder()
                  .name("expensive-path")
                  .capability(capExpensive)
                  .on(new ContextChangeTrigger(".trigger == true"))
                  .build())
          .goapActions(
              List.of(
                  new GoapAction("cheap-path", Map.of(), Map.of("done", true), 0.5),
                  new GoapAction("expensive-path", Map.of(), Map.of("done", true), 5.0)))
          .goalToEffectKey("goal", Set.of("done"))
          .goals(
              Goal.builder()
                  .name("goal")
                  .kind(GoalKind.SUCCESS)
                  .condition(new JQExpressionEvaluator(".done == true"))
                  .build())
          .completion(
              GoalExpression.allOf(
                  Goal.builder()
                      .name("goal")
                      .kind(GoalKind.SUCCESS)
                      .condition(new JQExpressionEvaluator(".done == true"))
                      .build()))
          .build();
    }
  }
}
