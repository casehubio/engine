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

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Worker;
import io.casehub.blackboard.plan.CasePlanModel;
import io.casehub.blackboard.plan.CasePlanModelRegistry;
import io.casehub.blackboard.stage.Stage;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test bean: free-floating worker (not in any Stage) alongside a staged worker. Both must run. */
@ApplicationScoped
public class MixedCaseHub extends CaseHub {

  static final AtomicBoolean stagedWorkerRan = new AtomicBoolean(false);
  static final AtomicBoolean freeWorkerRan = new AtomicBoolean(false);

  @Inject CasePlanModelRegistry planModelRegistry;

  private final CaseDefinition definition;

  public MixedCaseHub() {
    Capability stagedCap =
        Capability.builder()
            .name("staged-cap")
            .inputSchema("{ status: .status }")
            .outputSchema("{ stagedDone: .stagedDone }")
            .build();

    Capability freeCap =
        Capability.builder()
            .name("free-cap")
            .inputSchema("{ status: .status }")
            .outputSchema("{ freeDone: .freeDone }")
            .build();

    Goal goal =
        Goal.builder()
            .name("mixed-goal")
            .condition(".stagedDone == true and .freeDone == true")
            .kind(GoalKind.SUCCESS)
            .build();

    Worker stagedWorker =
        Worker.builder()
            .name("staged-worker")
            .capabilities(stagedCap)
            .function(
                input -> {
                  stagedWorkerRan.set(true);
                  return Map.of("stagedDone", true);
                })
            .build();

    Worker freeWorker =
        Worker.builder()
            .name("free-worker")
            .capabilities(freeCap)
            .function(
                input -> {
                  freeWorkerRan.set(true);
                  return Map.of("freeDone", true);
                })
            .build();

    definition =
        CaseDefinition.builder()
            .namespace("blackboard-test")
            .name("mixed-test")
            .version("1.0.0")
            .capabilities(stagedCap, freeCap)
            .workers(stagedWorker, freeWorker)
            .bindings(
                Binding.builder()
                    .name("trigger-staged")
                    .capability(stagedCap)
                    .on(new ContextChangeTrigger(".status == \"go\""))
                    .build(),
                Binding.builder()
                    .name("trigger-free")
                    .capability(freeCap)
                    .on(new ContextChangeTrigger(".status == \"go\""))
                    .build())
            .goals(goal)
            .completion(GoalExpression.allOf(goal))
            .build();
  }

  @PostConstruct
  void registerPlanModel() {
    Worker stagedWorker = definition.getWorkers().get(0);

    CasePlanModel planModel =
        CasePlanModel.builder()
            .name("mixed-plan")
            .stages(
                Stage.builder()
                    .name("staged-section")
                    .entry(".status == \"go\"")
                    .workers(stagedWorker)
                    .build())
            .build();

    // Only the staged worker is in the plan model; freeWorker is not — so it's free-floating
    planModelRegistry.register(definition, planModel);
  }

  @Override
  public CaseDefinition getDefinition() {
    return definition;
  }
}
