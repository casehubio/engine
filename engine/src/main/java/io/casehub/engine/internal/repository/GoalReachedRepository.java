package io.casehub.engine.internal.repository;

import io.casehub.engine.internal.model.GoalReached;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

/**
 * Repository for querying GoalReachedRecord using Hibernate Reactive.
 */
@ApplicationScoped
public class GoalReachedRepository {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * Persist a new GoalReachedRecord.
     *
     * @param record the goal reached record to persist
     * @return a Uni containing the persisted record
     */
    public Uni<GoalReached> persistReachedGoal(GoalReached record) {
        return sessionFactory.withTransaction((session, tx) ->
                session.persist(record)
                        .replaceWith(record)
        );
    }

    /**
     * Find all reached goals for a specific CaseHub run.
     *
     * @param runId the run ID
     * @return a Uni containing the list of reached goals
     */
    public Uni<List<GoalReached>> findByRunId(UUID runId) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "FROM GoalReachedRecord WHERE runId = :runId ORDER BY reachedAt ASC",
                        GoalReached.class)
                        .setParameter("runId", runId)
                        .getResultList()
        );
    }

    /**
     * Find all goal names that have been reached for a specific run.
     *
     * @param runId the run ID
     * @return a Uni containing the list of reached goal names
     */
    public Uni<List<String>> findReachedGoalNamesByRunId(UUID runId) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "SELECT goalName FROM GoalReachedRecord WHERE runId = :runId",
                        String.class)
                        .setParameter("runId", runId)
                        .getResultList()
        );
    }

    /**
     * Check if a specific goal has been reached for a run.
     *
     * @param runId the run ID
     * @param goalName the goal name
     * @return a Uni containing true if the goal has been reached, false otherwise
     */
    public Uni<Boolean> isGoalReached(UUID runId, String goalName) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "SELECT COUNT(g) FROM GoalReachedRecord g WHERE g.runId = :runId AND g.goalName = :goalName",
                        Long.class)
                        .setParameter("runId", runId)
                        .setParameter("goalName", goalName)
                        .getSingleResult()
                        .map(count -> count > 0)
        );
    }
}
