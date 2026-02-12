package io.casehub.engine.internal.engine.handler;

import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.GoalReachedEvent;
import io.casehub.engine.internal.model.GoalReached;
import io.casehub.engine.internal.repository.CaseCompletionRepository;
import io.casehub.engine.internal.repository.GoalReachedRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Handles GoalReachedEvent by persisting goal achievements and checking completion conditions.
 * <p>
 * This handler is a thin coordinator that delegates all business logic to specialized repositories:
 * - GoalReachedRepository: persists goal records
 * - CaseCompletionRepository: checks completion conditions and updates status
 * <p>
 * Following the Single Responsibility Principle - the handler only coordinates, doesn't implement logic.
 */
@ApplicationScoped
public class GoalReachedEventHandler {

  private static final Logger LOG = Logger.getLogger(GoalReachedEventHandler.class);

  @Inject
  GoalReachedRepository goalReachedRepository;

  @Inject
  CaseCompletionRepository completionRepository;

  /**
   * Processes GoalReachedEvent by:
   * 1. Persisting goal record to database
   * 2. Checking completion conditions and updating case status if needed
   *
   * @param event the goal reached event
   * @return Uni signaling completion
   */
  @ConsumeEvent(value = EventBusAddresses.GOAL_REACHED, ordered = true)
  public Uni<Void> onGoalReached(GoalReachedEvent event) {
    LOG.infof("Goal '%s' reached for runId: %s", event.goal().getName(), event.getRunId());

    GoalReached goalReached = toRecord(event);

    // Step 1: Persist goal record
    return goalReachedRepository.persistReachedGoal(goalReached)
            .invoke(persisted ->
                    LOG.infof("Goal '%s' (id: %d) persisted for runId: %s",
                            persisted.getGoalName(), persisted.getId(), persisted.getRunId())
            )
            // Step 2: Check completion conditions
            .chain(() -> completionRepository.checkAndUpdateCompletion(
                    event.getRunId(),
                    event.runState().getCaseHub())
            )
            .onFailure().invoke(e ->
                    LOG.errorf(e, "Failed to process goal '%s' for runId: %s",
                            event.goal().getName(), event.getRunId())
            );
  }

  /**
   * Converts GoalReachedEvent to persistent GoalReached record.
   */
  private GoalReached toRecord(GoalReachedEvent event) {
    GoalReached record = new GoalReached();
    record.setRunId(event.getRunId());
    record.setGoalName(event.goal().getName());
    record.setGoalDescription(event.goal().getDescription());
    record.setGoalKind(event.goal().getKind() != null ? event.goal().getKind().value() : null);
    record.setGoalCondition(event.goal().getCondition());
    record.setTerminal(event.goal().getTerminal());
    record.setContextSnapshot(event.runState().getStateContext().asJsonNode());

    return record;
  }
}
