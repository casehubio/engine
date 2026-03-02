package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.event.CaseStatusChanged;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseState;
import io.casehub.model.CaseCompletion;
import io.casehub.model.CaseHubDefinition;
import io.casehub.model.Goal;
import io.casehub.model.GoalExpression;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.casehub.engine.internal.history.CaseHubEventType.GOAL_REACHED;

@ApplicationScoped
public class GoalReachedEventHandler {

  private static final Logger LOG = Logger.getLogger(GoalReachedEventHandler.class);

  @Inject
  CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject
  EventBus eventBus;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ConsumeEvent(value = EventBusAddresses.GOAL_REACHED)
  public Uni<Void> onGoalReachedEventHandler(GoalReachedEvent event) {
    CaseInstance caseInstance = event.caseInstance();
    CaseHubDefinition definition = caseDefinitionRegistry.getCaseDefinition(caseInstance.getCaseDefinition());
    Goal goal = event.goal();

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(caseInstance.getUuid());
    eventLog.setEventType(GOAL_REACHED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
            .put("name", goal.getName())
            .put("description", goal.getDescription())
            .put("kind", goal.getKind().name())
            .put("isTerminal", goal.getTerminal())
    );

    CaseCompletion completion = definition.getSpec().getCompletion();

    return Panache.withTransaction(eventLog::persist)
            .chain(() -> evaluateCompletion(caseInstance, completion));
  }

  private Uni<Void> evaluateCompletion(CaseInstance caseInstance, CaseCompletion completion) {
    if (completion == null) {
      return Uni.createFrom().voidItem();
    }

    return Panache.withTransaction(() ->
            EventLog.find("caseId = ?1 and eventType = ?2", caseInstance.getUuid(), GOAL_REACHED)
                    .<EventLog>list()
    ).chain(eventLogs -> {
      Set<String> reachedGoals = eventLogs.stream()
              .map(el -> el.getMetadata().get("name").asText())
              .collect(Collectors.toSet());

      LOG.infof("Evaluating completion for caseId=%s, reachedGoals=%s", caseInstance.getUuid(), reachedGoals);

      String oldStatus = caseInstance.getState().name();

      if (completion.getFailure() != null && isGoalExpressionSatisfied(completion.getFailure(), reachedGoals)) {
        LOG.infof("Case FAILED: caseId=%s", caseInstance.getUuid());
        eventBus.publish(EventBusAddresses.CASE_STATUS_CHANGED,
                new CaseStatusChanged(caseInstance, oldStatus, CaseState.FAILED.name()));
        return Uni.createFrom().voidItem();
      }

      if (completion.getSuccess() != null && isGoalExpressionSatisfied(completion.getSuccess(), reachedGoals)) {
        LOG.infof("Case COMPLETED: caseId=%s", caseInstance.getUuid());
        eventBus.publish(EventBusAddresses.CASE_STATUS_CHANGED,
                new CaseStatusChanged(caseInstance, oldStatus, CaseState.COMPLETED.name()));
        return Uni.createFrom().voidItem();
      }

      return Uni.createFrom().voidItem();
    });
  }

  private boolean isGoalExpressionSatisfied(GoalExpression expression, Set<String> reachedGoals) {
    List<String> allOf = expression.getAllOf();
    if (allOf != null && !allOf.isEmpty()) {
      return reachedGoals.containsAll(allOf);
    }

    List<String> anyOf = expression.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      return anyOf.stream().anyMatch(reachedGoals::contains);
    }

    return false;
  }
}
