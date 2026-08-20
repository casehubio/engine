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

import io.casehub.api.context.ContextLayer;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.internal.event.CaseContextChangedEvent;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.event.OutcomeDisposition;
import io.casehub.engine.common.internal.event.WorkerOutcomeResolvedEvent;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.engine.planning.plan.CasePlanModel;
import io.casehub.engine.planning.registry.BlackboardRegistry;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

public class WorkerOutcomeResolvedHandler {

  private static final Logger LOG = Logger.getLogger(WorkerOutcomeResolvedHandler.class);

  private final BlackboardRegistry registry;
  private final CompoundCompletionEvaluator compoundCompletionEvaluator;
  private final EventBus eventBus;
  private final Event<PlanItemStateChangedEvent> planItemStateChangedEvents;

  @jakarta.inject.Inject
  jakarta.enterprise.inject.Instance<
          io.casehub.engine.planning.adaptation.DeeperDecompositionHandler>
      deeperDecompositionHandler;

  @Inject
  public WorkerOutcomeResolvedHandler(
      BlackboardRegistry registry,
      CompoundCompletionEvaluator compoundCompletionEvaluator,
      EventBus eventBus,
      Event<PlanItemStateChangedEvent> planItemStateChangedEvents) {
    this.registry = registry;
    this.compoundCompletionEvaluator = compoundCompletionEvaluator;
    this.eventBus = eventBus;
    this.planItemStateChangedEvents = planItemStateChangedEvents;
  }

  @ConsumeEvent(value = EventBusAddresses.WORKER_OUTCOME_RESOLVED, blocking = true)
  public void onWorkerOutcomeResolved(WorkerOutcomeResolvedEvent event) {
    CasePlanModel plan = registry.get(event.caseInstance().getUuid()).orElse(null);
    if (plan == null) {
      return;
    }

    plan.getPlanItemByBindingName(event.bindingName())
        .ifPresent(
            item -> {
              if (item.getStatus() != TaskStatus.RUNNING) {
                LOG.debugf(
                    "PlanItem for binding '%s' has status %s — not RUNNING, skipping",
                    event.bindingName(), item.getStatus());
                return;
              }

              // Deeper decomposition check — BEFORE markFaulted()
              if (event.disposition() == OutcomeDisposition.EXHAUSTED
                  && event.category() instanceof io.casehub.api.model.FailureCategory.Knowledge k
                  && deeperDecompositionHandler.isResolvable()) {
                boolean decomposed =
                    deeperDecompositionHandler
                        .get()
                        .tryDecompose(event.caseInstance(), plan, item, k);
                if (decomposed) {
                  planItemStateChangedEvents.fireAsync(
                      new PlanItemStateChangedEvent(
                          event.caseInstance().getUuid(),
                          item.getPlanItemId(),
                          item.getBindingName(),
                          TaskStatus.RUNNING,
                          TaskStatus.OBSOLETE,
                          event.caseInstance().tenancyId));
                  eventBus.publish(
                      EventBusAddresses.CONTEXT_CHANGED,
                      new CaseContextChangedEvent(
                          event.caseInstance(),
                          event.caseInstance().getCaseContext().snapshot(),
                          ContextLayer.WORKING));
                  LOG.infof(
                      "PlanItem '%s' decomposed deeper for binding '%s'",
                      item.getPlanItemId(), event.bindingName());
                  return;
                }
              }

              // Existing fault path
              TaskStatus prevStatus = item.getStatus();
              item.markFaulted();
              planItemStateChangedEvents.fireAsync(
                  new PlanItemStateChangedEvent(
                      event.caseInstance().getUuid(),
                      item.getPlanItemId(),
                      item.getBindingName(),
                      prevStatus,
                      TaskStatus.FAULTED,
                      event.caseInstance().tenancyId));

              if (event.disposition() == OutcomeDisposition.EXHAUSTED
                  || event.disposition() == OutcomeDisposition.FAULT) {
                compoundCompletionEvaluator.evaluate(
                    event.caseInstance().getUuid(),
                    event.caseInstance().tenancyId,
                    plan,
                    item.getBindingName());
              }

              if (event.disposition() != OutcomeDisposition.FAULT) {
                eventBus.publish(
                    EventBusAddresses.CONTEXT_CHANGED,
                    new CaseContextChangedEvent(
                        event.caseInstance(),
                        event.caseInstance().getCaseContext().snapshot(),
                        ContextLayer.WORKING));
              }

              LOG.infof(
                  "PlanItem '%s' marked FAULTED for binding '%s' — disposition=%s",
                  item.getPlanItemId(), event.bindingName(), event.disposition());
            });
  }
}
