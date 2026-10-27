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
package io.casehub.engine.planning.handler;

import io.casehub.api.model.BindingRequires;
import io.casehub.api.model.ExecutorRef;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.engine.planning.plan.CasePlanModel;
import io.casehub.engine.planning.plan.PlanItem;
import java.util.Optional;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

public class RequiresThresholdEvaluator {

  private static final Logger LOG = Logger.getLogger(RequiresThresholdEvaluator.class);

  private final CompoundCompletionEvaluator compoundCompletionEvaluator;
  private final Consumer<PlanItemStateChangedEvent> stateChangedEvents;

  public RequiresThresholdEvaluator(
      CompoundCompletionEvaluator compoundCompletionEvaluator,
      Consumer<PlanItemStateChangedEvent> stateChangedEvents) {
    this.compoundCompletionEvaluator = compoundCompletionEvaluator;
    this.stateChangedEvents = stateChangedEvents;
  }

  public void onPlanItemStateChanged(CasePlanModel plan, PlanItemStateChangedEvent event) {
    if (plan == null) return;

    if (event.newStatus() == TaskStatus.COMPLETED) {
      evaluateThresholdReached(event.caseId(), event.tenancyId(), plan, event.bindingName());
    } else if (event.newStatus().isTerminal()) {
      evaluateUnreachable(event.caseId(), event.tenancyId(), plan, event.bindingName());
    }
  }

  private void evaluateThresholdReached(
      java.util.UUID caseId, String tenancyId, CasePlanModel plan, String completedBindingName) {
    for (var entry : plan.getRequiresMetadata().entrySet()) {
      String gatedBindingName = entry.getKey();
      BindingRequires req = entry.getValue();
      if (!req.bindings().contains(completedBindingName)) continue;
      if (req.onThresholdReached() != OnThresholdReached.CANCEL) continue;

      long completed = countCompleted(req, plan);
      if (completed >= req.atLeast() && !plan.isRequiresThresholdTriggered(gatedBindingName)) {
        plan.markRequiresThresholdTriggered(gatedBindingName);
        cancelRemaining(caseId, tenancyId, req, plan);
        LOG.debugf(
            "Requires threshold reached for '%s' in case %s — cancelling remaining",
            gatedBindingName, caseId);
      }
    }
  }

  private void cancelRemaining(
      java.util.UUID caseId, String tenancyId, BindingRequires req, CasePlanModel plan) {
    req.bindings().stream()
        .map(plan::findPlanItemByBindingName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(pi -> !pi.getStatus().isTerminal())
        .forEach(
            pi -> {
              TaskStatus prev = pi.getStatus();
              pi.markCancelled();
              stateChangedEvents.accept(
                  new PlanItemStateChangedEvent(
                      caseId, pi.id(), pi.getBindingName(), prev, TaskStatus.CANCELLED, tenancyId));
              compoundCompletionEvaluator.evaluate(caseId, tenancyId, plan, pi.getBindingName());
            });
  }

  private void evaluateUnreachable(
      java.util.UUID caseId, String tenancyId, CasePlanModel plan, String faultedBindingName) {
    for (var entry : plan.getRequiresMetadata().entrySet()) {
      String gatedBindingName = entry.getKey();
      BindingRequires req = entry.getValue();
      if (!req.bindings().contains(faultedBindingName)) continue;

      long completed = countCompleted(req, plan);
      long nonCompletableTerminal = countNonCompletableTerminal(req, plan);
      long remaining = req.bindings().size() - completed - nonCompletableTerminal;
      long needed = req.atLeast() - completed;

      if (remaining < needed) {
        PlanItem pi = PlanItem.create(gatedBindingName, ExecutorRef.NONE, 0);
        if (!plan.addPlanItemIfAbsent(pi)) {
          var existing = plan.findPlanItemByBindingName(gatedBindingName);
          if (existing.isPresent() && existing.get().getStatus().isTerminal()) continue;
          if (existing.isPresent()) pi = existing.get();
        }
        pi.markFaulted();
        stateChangedEvents.accept(
            new PlanItemStateChangedEvent(
                caseId,
                pi.id(),
                gatedBindingName,
                TaskStatus.PENDING,
                TaskStatus.FAULTED,
                tenancyId));
        compoundCompletionEvaluator.evaluate(caseId, tenancyId, plan, gatedBindingName);
        LOG.debugf("Unreachable quorum for '%s' in case %s — faulted", gatedBindingName, caseId);
      }
    }
  }

  private long countCompleted(BindingRequires req, CasePlanModel plan) {
    return req.bindings().stream()
        .map(plan::findPlanItemByBindingName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(pi -> pi.getStatus() == TaskStatus.COMPLETED)
        .count();
  }

  private long countNonCompletableTerminal(BindingRequires req, CasePlanModel plan) {
    return req.bindings().stream()
        .map(plan::findPlanItemByBindingName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(pi -> pi.getStatus().isTerminal() && pi.getStatus() != TaskStatus.COMPLETED)
        .count();
  }
}
