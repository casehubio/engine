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

import static io.casehub.engine.internal.history.CaseHubEventType.GOAL_REACHED;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.CaseCompletion;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalExpression;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.event.CaseStatusChanged;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GoalReachedEventHandler {

  private static final Logger LOG = Logger.getLogger(GoalReachedEventHandler.class);

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject EventBus eventBus;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ConsumeEvent(value = EventBusAddresses.GOAL_REACHED)
  public Uni<Void> onGoalReachedEventHandler(GoalReachedEvent event) {
    CaseInstance caseInstance = event.caseInstance();
    CaseDefinition definition =
        caseDefinitionRegistry.getCaseDefinition(caseInstance.getCaseMetaModel());
    Goal goal = event.goal();

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(caseInstance.getUuid());
    eventLog.setEventType(GOAL_REACHED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setMetadata(
        OBJECT_MAPPER
            .createObjectNode()
            .put("name", goal.getName())
            .put("description", goal.getDescription())
            .put("kind", goal.getKind().value())
            .put("isTerminal", goal.getTerminal()));

    CaseCompletion completion = definition.getCompletion();

    return Panache.withTransaction(eventLog::persist)
        .chain(() -> evaluateCompletion(caseInstance, completion));
  }

  private Uni<Void> evaluateCompletion(CaseInstance caseInstance, CaseCompletion completion) {
    if (completion == null) {
      return Uni.createFrom().voidItem();
    }

    if (!(completion instanceof GoalBasedCompletion goalBasedCompletion)) {
      return Uni.createFrom().voidItem();
    }

    return Panache.withTransaction(
            () ->
                EventLog.find(
                        "caseId = ?1 and eventType = ?2", caseInstance.getUuid(), GOAL_REACHED)
                    .<EventLog>list())
        .chain(
            eventLogs -> {
              Set<String> reachedGoals =
                  eventLogs.stream()
                      .map(el -> el.getMetadata().get("name").asText())
                      .collect(Collectors.toSet());

              LOG.infof(
                  "Evaluating completion for caseId=%s, reachedGoals=%s",
                  caseInstance.getUuid(), reachedGoals);

              String oldStatus = caseInstance.getState().name();

              if (goalBasedCompletion.getFailure() != null
                  && isGoalExpressionSatisfied(goalBasedCompletion.getFailure(), reachedGoals)) {
                LOG.infof("Case FAILED: caseId=%s", caseInstance.getUuid());
                eventBus.publish(
                    EventBusAddresses.CASE_STATUS_CHANGED,
                    new CaseStatusChanged(caseInstance, oldStatus, CaseStatus.FAULTED.name()));
                return Uni.createFrom().voidItem();
              }

              if (goalBasedCompletion.getSuccess() != null
                  && isGoalExpressionSatisfied(goalBasedCompletion.getSuccess(), reachedGoals)) {
                LOG.infof("Case COMPLETED: caseId=%s", caseInstance.getUuid());
                eventBus.publish(
                    EventBusAddresses.CASE_STATUS_CHANGED,
                    new CaseStatusChanged(caseInstance, oldStatus, CaseStatus.COMPLETED.name()));
                return Uni.createFrom().voidItem();
              }

              return Uni.createFrom().voidItem();
            });
  }

  private boolean isGoalExpressionSatisfied(GoalExpression expression, Set<String> reachedGoals) {
    if (expression == null || expression.getGoals() == null || expression.getGoals().isEmpty()) {
      return false;
    }

    Set<String> expressionGoalNames =
        expression.getGoals().stream().map(Goal::getName).collect(Collectors.toSet());

    if (expression instanceof io.casehub.api.model.AllOfGoalExpression) {
      return reachedGoals.containsAll(expressionGoalNames);
    }

    if (expression instanceof io.casehub.api.model.AnyOfGoalExpression) {
      return expressionGoalNames.stream().anyMatch(reachedGoals::contains);
    }

    return false;
  }
}
