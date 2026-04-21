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
package io.casehub.blackboard.strategy;

import io.casehub.api.engine.LoopControl;
import io.casehub.api.engine.PlanExecutionContext;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Worker;
import io.casehub.blackboard.plan.CasePlanModel;
import io.casehub.blackboard.plan.CasePlanModelRegistry;
import io.casehub.blackboard.plan.PlanItem;
import io.casehub.blackboard.plan.PlanItemStatus;
import io.casehub.blackboard.stage.Stage;
import io.casehub.engine.internal.engine.ExpressionEngineRegistry;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Blackboard-aware {@link LoopControl} that layers Stage orchestration on top of the engine's
 * choreography loop.
 *
 * <p>When a {@link CasePlanModel} is registered for the current case definition, this control gates
 * eligible Bindings through the Stage lifecycle: only Bindings whose Worker belongs to an ACTIVE
 * Stage are permitted. Bindings for Workers not assigned to any Stage pass through unchanged (pure
 * choreography).
 *
 * <p>When no plan model is registered, all eligible Bindings are returned — identical to {@link
 * io.casehub.engine.internal.engine.ChoreographyLoopControl}.
 */
@Alternative
@Priority(10)
@ApplicationScoped
public class PlanningStrategyLoopControl implements LoopControl {

  private static final Logger LOG = Logger.getLogger(PlanningStrategyLoopControl.class);

  private final Map<UUID, List<PlanItem<Stage>>> planInstances = new ConcurrentHashMap<>();

  @Inject CasePlanModelRegistry registry;
  @Inject PlanningStrategy planningStrategy;
  @Inject ExpressionEngineRegistry expressionEngineRegistry;

  @Override
  public Uni<List<Binding>> select(PlanExecutionContext context, List<Binding> eligible) {
    Optional<CasePlanModel> planModel = registry.find(context.definition());
    if (planModel.isEmpty()) {
      return Uni.createFrom().item(eligible); // no plan model — pure choreography
    }

    UUID caseId = context.caseId();
    List<PlanItem<Stage>> stageItems =
        planInstances.computeIfAbsent(caseId, id -> buildPlanInstance(planModel.get()));

    // 1. Evaluate entry criteria — activate PENDING stages
    for (PlanItem<Stage> item : stageItems) {
      evaluateEntry(item, context);
    }

    // 2. Evaluate exit criteria — complete ACTIVE stages with explicit exit condition
    for (PlanItem<Stage> item : stageItems) {
      evaluateExit(item, context);
    }

    // 3. Collect workers in ACTIVE stages
    Map<String, Worker> activeStageWorkers = new HashMap<>();
    for (PlanItem<Stage> item : stageItems) {
      collectActiveWorkers(item, activeStageWorkers);
    }

    // 4. Split eligible bindings: staged vs free-floating
    List<Binding> stagedBindings = new ArrayList<>();
    List<Binding> freeFloatingBindings = new ArrayList<>();

    for (Binding binding : eligible) {
      if (binding.getCapability() == null) {
        freeFloatingBindings.add(binding);
        continue;
      }
      String cap = binding.getCapability().getName();
      boolean inActiveStage =
          activeStageWorkers.values().stream().anyMatch(w -> workerHasCapability(w, cap));
      boolean inAnyStage = isCapabilityInAnyStage(cap, planModel.get());

      if (!inAnyStage) {
        freeFloatingBindings.add(binding); // choreographic — not in any stage
      } else if (inActiveStage) {
        stagedBindings.add(binding); // orchestrated and stage is active
      }
      // else: in a non-active stage — filtered out
    }

    // 5. Resolve staged bindings to PlanItems, delegate to PlanningStrategy
    List<PlanItem<?>> eligibleItems = new ArrayList<>();
    for (Binding binding : stagedBindings) {
      String cap = binding.getCapability().getName();
      activeStageWorkers.values().stream()
          .filter(w -> workerHasCapability(w, cap))
          .findFirst()
          .ifPresent(
              w -> {
                PlanItem<Worker> workerItem = findWorkerItem(w, stageItems);
                if (workerItem != null && !eligibleItems.contains(workerItem)) {
                  eligibleItems.add(workerItem);
                }
              });
    }

    List<PlanItem<?>> selected = planningStrategy.select(context.caseContext(), eligibleItems);

    // 6. Translate selected PlanItems back to Bindings
    List<Binding> result = new ArrayList<>(freeFloatingBindings);
    for (PlanItem<?> selectedItem : selected) {
      if (!(selectedItem.getElement() instanceof Worker selectedWorker)) continue;
      for (Binding binding : stagedBindings) {
        if (binding.getCapability() != null
            && workerHasCapability(selectedWorker, binding.getCapability().getName())) {
          result.add(binding);
          break;
        }
      }
    }

    return Uni.createFrom().item(result);
  }

  // ---- plan instance construction ----

  private List<PlanItem<Stage>> buildPlanInstance(CasePlanModel model) {
    List<PlanItem<Stage>> rootItems = new ArrayList<>();
    for (Stage stage : model.getStages()) {
      rootItems.add(buildStageItem(stage, null));
    }
    return rootItems;
  }

  private PlanItem<Stage> buildStageItem(Stage stage, PlanItem<?> parent) {
    PlanItem<Stage> item = PlanItem.of(stage);
    if (parent != null) {
      parent.addChild(item);
    }
    for (Worker worker : stage.getWorkers()) {
      item.addChild(PlanItem.of(worker));
    }
    for (Stage nested : stage.getNestedStages()) {
      buildStageItem(nested, item);
    }
    return item;
  }

  // ---- entry/exit condition evaluation ----

  private void evaluateEntry(PlanItem<Stage> item, PlanExecutionContext context) {
    if (item.getStatus() == PlanItemStatus.PENDING) {
      Stage stage = item.getElement();
      if (expressionEngineRegistry.evaluate(stage.getEntryCondition(), context.caseContext())) {
        LOG.infof("Stage '%s' entry condition met — activating", stage.getName());
        item.activate();
        // activate worker children
        for (PlanItem<?> child : item.getChildren()) {
          if (child.getElement() instanceof Worker && child.getStatus() == PlanItemStatus.PENDING) {
            child.activate();
          }
        }
      }
    }
    // recurse into nested stages only when parent is ACTIVE
    if (item.getStatus() == PlanItemStatus.ACTIVE) {
      for (PlanItem<?> child : item.getChildren()) {
        if (child.getElement() instanceof Stage) {
          @SuppressWarnings("unchecked")
          PlanItem<Stage> nestedStage = (PlanItem<Stage>) child;
          evaluateEntry(nestedStage, context);
        }
      }
    }
  }

  private void evaluateExit(PlanItem<Stage> item, PlanExecutionContext context) {
    if (item.getStatus() != PlanItemStatus.ACTIVE) return;
    Stage stage = item.getElement();
    if (stage.getExitCondition() != null
        && expressionEngineRegistry.evaluate(stage.getExitCondition(), context.caseContext())) {
      LOG.infof("Stage '%s' exit condition met — completing", stage.getName());
      item.complete();
    }
    // recurse into active nested stages
    for (PlanItem<?> child : item.getChildren()) {
      if (child.getElement() instanceof Stage) {
        @SuppressWarnings("unchecked")
        PlanItem<Stage> nestedStage = (PlanItem<Stage>) child;
        evaluateExit(nestedStage, context);
      }
    }
  }

  // ---- worker collection and matching ----

  private void collectActiveWorkers(PlanItem<Stage> item, Map<String, Worker> result) {
    if (item.getStatus() != PlanItemStatus.ACTIVE) return;
    for (PlanItem<?> child : item.getChildren()) {
      if (child.getElement() instanceof Worker w && child.getStatus() == PlanItemStatus.ACTIVE) {
        result.put(w.getName(), w);
      } else if (child.getElement() instanceof Stage) {
        @SuppressWarnings("unchecked")
        PlanItem<Stage> nestedStage = (PlanItem<Stage>) child;
        collectActiveWorkers(nestedStage, result);
      }
    }
  }

  private boolean workerHasCapability(Worker worker, String capabilityName) {
    return worker.getCapabilities().stream().anyMatch(c -> c.getName().equals(capabilityName));
  }

  private boolean isCapabilityInAnyStage(String capabilityName, CasePlanModel model) {
    return model.getStages().stream().anyMatch(s -> stageContainsCapability(s, capabilityName));
  }

  private boolean stageContainsCapability(Stage stage, String capabilityName) {
    for (Worker w : stage.getWorkers()) {
      if (workerHasCapability(w, capabilityName)) return true;
    }
    for (Stage nested : stage.getNestedStages()) {
      if (stageContainsCapability(nested, capabilityName)) return true;
    }
    return false;
  }

  private PlanItem<Worker> findWorkerItem(Worker worker, List<PlanItem<Stage>> stageItems) {
    for (PlanItem<Stage> stageItem : stageItems) {
      PlanItem<Worker> found = findWorkerItemInStage(worker, stageItem);
      if (found != null) return found;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private PlanItem<Worker> findWorkerItemInStage(Worker worker, PlanItem<Stage> stageItem) {
    for (PlanItem<?> child : stageItem.getChildren()) {
      if (child.getElement() instanceof Worker w && w.getName().equals(worker.getName())) {
        return (PlanItem<Worker>) child;
      } else if (child.getElement() instanceof Stage) {
        PlanItem<Worker> found = findWorkerItemInStage(worker, (PlanItem<Stage>) child);
        if (found != null) return found;
      }
    }
    return null;
  }

  /** Evict cached plan instance for a completed or terminated case. */
  public void evictCase(UUID caseId) {
    planInstances.remove(caseId);
  }
}
