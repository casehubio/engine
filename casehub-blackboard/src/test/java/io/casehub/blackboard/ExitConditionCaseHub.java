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

/**
 * Test bean: stage with explicit exit condition. Worker writes status that triggers both exit and
 * goal.
 */
@ApplicationScoped
public class ExitConditionCaseHub extends CaseHub {

  @Inject CasePlanModelRegistry planModelRegistry;

  private final CaseDefinition definition;

  public ExitConditionCaseHub() {
    Capability cap =
        Capability.builder()
            .name("exit-cond-cap")
            .inputSchema("{ status: .status }")
            .outputSchema("{ status: .status }")
            .build();

    Goal goal =
        Goal.builder()
            .name("exited-goal")
            .condition(".status == \"exited\"")
            .kind(GoalKind.SUCCESS)
            .build();

    Worker worker =
        Worker.builder()
            .name("exit-cond-worker")
            .capabilities(cap)
            .function(input -> Map.of("status", "exited"))
            .build();

    definition =
        CaseDefinition.builder()
            .namespace("blackboard-test")
            .name("exit-condition-test")
            .version("1.0.0")
            .capabilities(cap)
            .workers(worker)
            .bindings(
                Binding.builder()
                    .name("trigger-exit")
                    .capability(cap)
                    .on(new ContextChangeTrigger(".status == \"start\""))
                    .build())
            .goals(goal)
            .completion(GoalExpression.allOf(goal))
            .build();
  }

  @PostConstruct
  void registerPlanModel() {
    Worker worker = definition.getWorkers().get(0);

    CasePlanModel planModel =
        CasePlanModel.builder()
            .name("exit-condition-plan")
            .stages(
                Stage.builder()
                    .name("start-stage")
                    .entry(".status == \"start\"")
                    .exit(".status == \"exited\"")
                    .workers(worker)
                    .build())
            .build();

    planModelRegistry.register(definition, planModel);
  }

  @Override
  public CaseDefinition getDefinition() {
    return definition;
  }
}
