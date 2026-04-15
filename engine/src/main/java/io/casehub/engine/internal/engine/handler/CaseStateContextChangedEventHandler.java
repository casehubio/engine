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
package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.engine.LoopControl;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.Milestone;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.ExpressionEngineRegistry;
import io.casehub.engine.internal.event.CaseStateContextChangedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.event.MilestoneReachedEvent;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.engine.internal.model.CaseState;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CaseStateContextChangedEventHandler {

  private static final Logger LOG = Logger.getLogger(CaseStateContextChangedEventHandler.class);

  @Inject EventBus eventBus;

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject ExpressionEngineRegistry expressionEngineRegistry;

  @Inject LoopControl loopControl;

  @ConsumeEvent(value = EventBusAddresses.CONTEXT_CHANGED)
  public Uni<Void> onCaseStateContextChangedEventHandler(CaseStateContextChangedEvent event) {
    CaseInstance caseInstance = event.instance();
    JsonNode contextSnapshot = event.contextSnapshot();
    if (!caseInstance.getState().equals(CaseState.ACTIVE)) {
      return Uni.createFrom().voidItem();
    }

    CaseMetaModel caseMetaModel = caseInstance.getCaseMetaModel();
    CaseDefinition caseefinition = caseDefinitionRegistry.getCaseDefinition(caseMetaModel);

    if (caseefinition == null) {
      return Uni.createFrom()
          .failure(
              new RuntimeException(
                  "Case definition not found for caseId: " + caseInstance.getUuid()));
    }

    LOG.infof("Handling CaseStateContextChangedEvent for caseId: %s", caseInstance.getUuid());

    return rules(caseInstance, contextSnapshot, caseefinition)
        .chain(() -> milestones(caseInstance, contextSnapshot, caseefinition))
        .chain(() -> goals(caseInstance, contextSnapshot, caseefinition))
        .invoke(
            () ->
                LOG.debugf(
                    "Rules+milestones+goals processed for caseId: %s", caseInstance.getUuid()))
        .onFailure()
        .invoke(
            t ->
                LOG.errorf(
                    t, "Failed handling context changed for caseId: %s", caseInstance.getUuid()));
  }

  private Uni<Void> rules(
      CaseInstance caseInstance, JsonNode contextSnapshot, CaseDefinition definition) {
    List<Binding> bindings = definition.getBindings();
    if (bindings == null || bindings.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    List<Worker> workers = definition.getWorkers();

    // Evaluate trigger conditions to find eligible rules
    List<Binding> eligible = new ArrayList<>();
    for (Binding binding : bindings) {
      if (!(binding.getOn() instanceof ContextChangeTrigger cct)) {
        continue;
      }

      if (!expressionEngineRegistry.evaluate(cct.getFilter(), contextSnapshot)) {
        continue;
      }

      if (binding.getCapability() == null) {
        LOG.warnf("Capability referenced by rule '%s' is null", binding.getName());
        continue;
      }

      eligible.add(binding);
    }

    // LoopControl decides which eligible rules to fire (default: all of them)
    List<Binding> selected = loopControl.select(caseInstance.getStateContext(), eligible);

    List<Uni<Void>> unis = new ArrayList<>(selected.size());
    for (Binding b : selected) {
      unis.add(publishWorkerSchedules(caseInstance, workers, b, b.getCapability()));
    }

    if (unis.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    return Uni.combine().all().unis(unis).discardItems();
  }

  private Uni<Void> milestones(
      CaseInstance caseInstance, JsonNode contextSnapshot, CaseDefinition definition) {
    List<Milestone> milestones = definition.getMilestones();
    if (milestones == null || milestones.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    for (Milestone milestone : milestones) {
      if (!expressionEngineRegistry.evaluate(
          milestone.getCondition(), caseInstance.getStateContext())) continue;

      LOG.infof("Milestone '%s' REACHED! Publishing MilestoneReachedEvent", milestone.getName());

      eventBus.publish(
          EventBusAddresses.MILESTONE_REACHED, new MilestoneReachedEvent(caseInstance, milestone));
    }

    return Uni.createFrom().voidItem();
  }

  private Uni<Void> goals(
      CaseInstance caseInstance, JsonNode contextSnapshot, CaseDefinition definition) {
    List<Goal> goals = definition.getGoals();
    if (goals == null || goals.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    for (Goal goal : goals) {
      if (!expressionEngineRegistry.evaluate(goal.getCondition(), caseInstance.getStateContext()))
        continue;

      LOG.infof("Goal '%s' REACHED! Publishing GoalReachedEvent", goal.getName());

      eventBus.publish(EventBusAddresses.GOAL_REACHED, new GoalReachedEvent(caseInstance, goal));
    }

    return Uni.createFrom().voidItem();
  }

  private Uni<Void> publishWorkerSchedules(
      CaseInstance caseInstance, List<Worker> workers, Binding binding, Capability capability) {

    if (workers == null || workers.isEmpty()) {
      LOG.warnf(
          "No workers defined; cannot schedule capability '%s' for rule '%s'",
          capability.getName(), binding.getName());
      return Uni.createFrom().voidItem();
    }

    for (Worker worker : workers) {
      if (worker.getCapabilities() == null) {
        continue;
      }
      if (worker.getCapabilities().stream()
          .noneMatch(c -> c.getName().equals(capability.getName()))) {
        continue;
      }

      LOG.infof(
          "Worker '%s' matches capability '%s' from rule '%s' -> scheduling",
          worker.getName(), capability.getName(), binding.getName());

      eventBus.publish(
          EventBusAddresses.WORKER_SCHEDULE,
          new WorkerScheduleEvent(caseInstance, worker, capability));
    }
    return Uni.createFrom().voidItem();
  }
}
