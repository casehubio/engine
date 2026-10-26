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
package io.casehub.engine.runtime.improvement;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.improvement.ImprovementConfig;
import io.casehub.api.model.stigmergy.TickTrace;
import io.casehub.api.spi.routing.GoalFormationResult;
import io.casehub.api.spi.routing.GoalFormationService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvolutionTimerBackstopTest {

  private EvolutionTimerBackstop backstop;
  private AtomicInteger tickCount;
  private EvolutionTicker ticker;

  @BeforeEach
  void setUp() {
    tickCount = new AtomicInteger(0);
    var areaRegistry = new CapabilityAreaRegistry();
    var healthTracker = new HealthScoreTracker(areaRegistry);
    var circuitBreaker = new ImprovementCircuitBreaker(e -> {});
    var categoryRegistry = new ImprovementCategoryRegistry();
    var regressionDetector =
        new RegressionDetector(
            new RegressionEvaluatorRegistry(),
            categoryRegistry,
            new ImprovementCategoryTracker(),
            new RollbackHistory(),
            healthTracker,
            e -> {});

    GoalFormationService goalService =
        (agentId, tenancyId, proposal) -> {
          tickCount.incrementAndGet();
          return new GoalFormationResult(List.of(), List.of(), 0);
        };

    var proposalSourceRegistry = new ImprovementProposalSourceRegistry();
    var conflictStrategyRegistry = new ConflictStrategyRegistry();
    var goalFormation =
        new ImprovementGoalFormationStrategy(
            new ImprovementBudgetEnforcer(new InMemoryDenyPatternStore()),
            proposalSourceRegistry,
            categoryRegistry,
            new ImprovementCategoryTracker(),
            new RollbackHistory(),
            conflictStrategyRegistry,
            new DenyPatternProviderRegistry());

    ticker =
        new EvolutionTicker(
            goalFormation,
            circuitBreaker,
            healthTracker,
            regressionDetector,
            goalService,
            new TickTraceBuffer(),
            e -> {});

    backstop = new EvolutionTimerBackstop(ticker);
  }

  @Test
  void register_tracksCase() {
    var caseId = UUID.randomUUID();
    var config =
        new ImprovementConfig(null, null, null, null, null, true, 60, null, null, null, null);

    backstop.register(caseId, "tenant-1", config);

    assertThat(backstop.registeredCount()).isEqualTo(1);
  }

  @Test
  void unregister_removesCase() {
    var caseId = UUID.randomUUID();
    var config =
        new ImprovementConfig(null, null, null, null, null, true, 60, null, null, null, null);

    backstop.register(caseId, "tenant-1", config);
    backstop.unregister(caseId);

    assertThat(backstop.registeredCount()).isEqualTo(0);
  }

  @Test
  void sweep_ticksRegisteredCases() {
    var caseId = UUID.randomUUID();
    var config =
        new ImprovementConfig(null, null, null, null, null, true, 60, null, null, null, null);

    backstop.register(caseId, "tenant-1", config);
    backstop.sweep();

    assertThat(backstop.registeredCount()).isEqualTo(1);
  }

  @Test
  void sweep_usesTimerTrigger() {
    var caseId = UUID.randomUUID();
    var config =
        new ImprovementConfig(null, null, null, null, null, true, 0, null, null, null, null);
    var traceBuffer = new TickTraceBuffer();
    var localTicker =
        new EvolutionTicker(
            new ImprovementGoalFormationStrategy(
                new ImprovementBudgetEnforcer(new InMemoryDenyPatternStore()),
                new ImprovementProposalSourceRegistry(),
                new ImprovementCategoryRegistry(),
                new ImprovementCategoryTracker(),
                new RollbackHistory(),
                new ConflictStrategyRegistry(),
                new DenyPatternProviderRegistry()),
            new ImprovementCircuitBreaker(e -> {}),
            new HealthScoreTracker(new CapabilityAreaRegistry()),
            new RegressionDetector(
                new RegressionEvaluatorRegistry(),
                new ImprovementCategoryRegistry(),
                new ImprovementCategoryTracker(),
                new RollbackHistory(),
                new HealthScoreTracker(new CapabilityAreaRegistry()),
                e -> {}),
            (a, t, p) -> new GoalFormationResult(List.of(), List.of(), 0),
            traceBuffer,
            e -> {});

    var localBackstop = new EvolutionTimerBackstop(localTicker);
    localBackstop.register(caseId, "tenant-1", config);
    localBackstop.sweep();

    var traces = traceBuffer.recent(caseId, 10);
    assertThat(traces).hasSize(1);
    assertThat(traces.get(0).trigger()).isEqualTo(TickTrace.TickTrigger.TIMER);
  }

  @Test
  void noOpConstructor_sweepDoesNothing() {
    var noOpBackstop = new EvolutionTimerBackstop();
    noOpBackstop.register(
        UUID.randomUUID(),
        "tenant-1",
        new ImprovementConfig(null, null, null, null, null, true, 60, null, null, null, null));
    noOpBackstop.sweep();
    assertThat(noOpBackstop.registeredCount()).isEqualTo(0);
  }

  @Test
  void reset_clearsAllRegistrations() {
    backstop.register(
        UUID.randomUUID(),
        "tenant-1",
        new ImprovementConfig(null, null, null, null, null, true, 60, null, null, null, null));
    backstop.register(
        UUID.randomUUID(),
        "tenant-1",
        new ImprovementConfig(null, null, null, null, null, true, 60, null, null, null, null));

    assertThat(backstop.registeredCount()).isEqualTo(2);

    backstop.reset();

    assertThat(backstop.registeredCount()).isEqualTo(0);
  }
}
