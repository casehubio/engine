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
package io.casehub.engine.internal.quarkus;

import io.casehub.api.spi.ActionRiskClassifier;
import io.casehub.api.spi.DispatchBudget;
import io.casehub.api.spi.FailureClassifier;
import io.casehub.api.spi.routing.WorkloadDataProvider;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.engine.common.spi.scheduler.WorkerExecutionRoutingStrategy;
import io.casehub.engine.internal.routing.FirstSupportedRoutingStrategy;
import io.casehub.engine.internal.routing.NoOpWorkloadDataProvider;
import io.casehub.engine.internal.worker.DefaultFailureClassifier;
import io.casehub.engine.internal.worker.NoOpCapabilityHealth;
import io.casehub.engine.internal.worker.NoOpDispatchBudget;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class RuntimeDefaultBeans {

  @Produces
  @DefaultBean
  DispatchBudget dispatchBudget() {
    return new NoOpDispatchBudget();
  }

  @Produces
  @DefaultBean
  FailureClassifier failureClassifier() {
    return new DefaultFailureClassifier();
  }

  @Produces
  @DefaultBean
  ActionRiskClassifier actionRiskClassifier() {
    return (action, context) -> new io.casehub.api.spi.RiskDecision.Autonomous();
  }

  @Produces
  @DefaultBean
  CapabilityHealth capabilityHealth() {
    return new NoOpCapabilityHealth();
  }

  @Produces
  @DefaultBean
  WorkloadDataProvider workloadDataProvider() {
    return new NoOpWorkloadDataProvider();
  }

  @Produces
  @DefaultBean
  WorkerExecutionRoutingStrategy workerExecutionRoutingStrategy() {
    return new FirstSupportedRoutingStrategy();
  }

  @Produces
  @DefaultBean
  io.casehub.api.spi.recovery.ErrorClassifier errorClassifier() {
    return new io.casehub.engine.internal.worker.DefaultErrorClassifier();
  }

  @Produces
  @DefaultBean
  io.casehub.engine.common.spi.PlanAdaptationEvaluator planAdaptationEvaluator() {
    return new io.casehub.engine.internal.worker.NoOpPlanAdaptationEvaluator();
  }

  @Produces
  @DefaultBean
  io.casehub.engine.common.spi.GoalDecomposer goalDecomposer() {
    return new io.casehub.engine.internal.worker.NoOpGoalDecomposer();
  }

  @Produces
  @DefaultBean
  io.casehub.api.engine.LoopControl loopControl() {
    return new io.casehub.engine.internal.engine.ChoreographyLoopControl();
  }
}
