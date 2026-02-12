package io.casehub.engine.internal.repository;

import io.casehub.engine.internal.engine.CaseHubDefinitionRegistry;
import io.casehub.engine.internal.model.CaseHub;
import io.casehub.engine.internal.model.CaseHubInstanceRun;
import io.casehub.engine.internal.model.GoalReached;
import io.casehub.model.CaseCompletion;
import io.casehub.model.CaseHubDefinition;
import io.casehub.model.GoalExpression;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class CaseCompletionRepository {

  private static final Logger LOG = Logger.getLogger(CaseCompletionRepository.class);

  @Inject
  Mutiny.SessionFactory sessionFactory;

  @Inject
  CaseHubDefinitionRegistry definitionRegistry;

  public Uni<Void> checkAndUpdateCompletion(UUID runId, CaseHub caseHub) {
    CaseCompletion completion = extractCompletionConditions(caseHub);

    if (completion == null) {
      LOG.debugf("No completion conditions for runId: %s", runId);
      return Uni.createFrom().voidItem();
    }

    return sessionFactory.withTransaction((session, tx) ->
            checkCompletionInTransaction(session, runId, completion)
    );
  }

  private Uni<Void> checkCompletionInTransaction(Mutiny.Session session, UUID runId, CaseCompletion completion) {
    LOG.infof("Checking completion for runId: %s", runId);

    return loadReachedGoalNames(session, runId)
            .chain(reachedGoals -> {
              LOG.infof("Reached goals for runId %s: %s", runId, reachedGoals);

              CaseHubInstanceRun.CaseHubStatus newStatus = determineNewStatus(completion, reachedGoals, runId);

              if (newStatus == null) {
                LOG.debugf("No completion conditions met for runId: %s", runId);
                return Uni.createFrom().voidItem();
              }

              return updateCaseStatus(session, runId, newStatus);
            });
  }

  private Uni<Set<String>> loadReachedGoalNames(Mutiny.Session session, UUID runId) {
    return session.createQuery(
            "SELECT goalName FROM GoalReachedRecord WHERE runId = :runId",
            String.class)
            .setParameter("runId", runId)
            .getResultList()
            .map(Set::copyOf);
  }

  private CaseHubInstanceRun.CaseHubStatus determineNewStatus(
          CaseCompletion completion,
          Set<String> reachedGoals,
          UUID runId) {

    if (completion.getSuccess() != null && evaluateGoalExpression(completion.getSuccess(), reachedGoals)) {
      LOG.infof("SUCCESS condition met for runId: %s", runId);
      return CaseHubInstanceRun.CaseHubStatus.COMPLETED;
    }

    if (completion.getFailure() != null && evaluateGoalExpression(completion.getFailure(), reachedGoals)) {
      LOG.infof("FAILURE condition met for runId: %s", runId);
      return CaseHubInstanceRun.CaseHubStatus.FAILED;
    }

    return null;
  }

  private Uni<Void> updateCaseStatus(Mutiny.Session session, UUID runId, CaseHubInstanceRun.CaseHubStatus newStatus) {
    return session.createQuery(
            "FROM CaseHubInstanceRun WHERE runId = :runId",
            CaseHubInstanceRun.class)
            .setParameter("runId", runId)
            .getSingleResultOrNull()
            .chain(run -> {
              if (run != null) {
                run.setStatus(newStatus);
                return session.merge(run)
                        .invoke(() -> LOG.infof("Status updated to %s for runId: %s", newStatus, runId))
                        .replaceWithVoid();
              }
              return Uni.createFrom().voidItem();
            });
  }

  private boolean evaluateGoalExpression(GoalExpression expression, Set<String> reachedGoals) {
    if (expression.getAllOf() != null && !expression.getAllOf().isEmpty()) {
      boolean allReached = expression.getAllOf().stream().allMatch(reachedGoals::contains);
      if (allReached) {
        LOG.debugf("allOf satisfied: %s", expression.getAllOf());
        return true;
      }
    }

    if (expression.getAnyOf() != null && !expression.getAnyOf().isEmpty()) {
      boolean anyReached = expression.getAnyOf().stream().anyMatch(reachedGoals::contains);
      if (anyReached) {
        LOG.debugf("anyOf satisfied: %s", expression.getAnyOf());
        return true;
      }
    }

    return false;
  }

  private CaseCompletion extractCompletionConditions(CaseHub caseHub) {
    try {
      CaseHubDefinition definition = definitionRegistry.getDefinition(caseHub);
      if (definition == null || definition.getSpec() == null) {
        return null;
      }
      return definition.getSpec().getCompletion();
    } catch (Exception e) {
      LOG.errorf(e, "Failed to extract completion conditions for CaseHub id: %s", caseHub.getId());
      return null;
    }
  }
}
