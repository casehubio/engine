package io.casehub.engine.internal.repository;

import io.casehub.engine.internal.model.CaseHubInstanceRun;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing CaseHub instance runs using Hibernate Reactive.
 */
@ApplicationScoped
public class CaseHubInstanceRunRepository {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * Persist a new CaseHub instance run.
     *
     * @param run the run to persist
     * @return a Uni containing the persisted run
     */
    public Uni<CaseHubInstanceRun> persist(CaseHubInstanceRun run) {
        return sessionFactory.withTransaction((session, tx) ->
                session.persist(run)
                        .replaceWith(run)
        );
    }

    /**
     * Update an existing CaseHub instance run.
     *
     * @param run the run to update
     * @return a Uni containing the updated run
     */
    public Uni<CaseHubInstanceRun> update(CaseHubInstanceRun run) {
        return sessionFactory.withTransaction((session, tx) ->
                session.merge(run)
        );
    }

    /**
     * Find a CaseHub instance run by its run ID with CaseHub eagerly fetched.
     *
     * @param runId the unique run identifier
     * @return a Uni containing the run, or null if not found
     */
    public Uni<CaseHubInstanceRun> findByRunId(UUID runId) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "FROM CaseHubInstanceRun r JOIN FETCH r.caseHub WHERE r.runId = :runId",
                        CaseHubInstanceRun.class)
                        .setParameter("runId", runId)
                        .getSingleResultOrNull()
        );
    }

    /**
     * Find all runs for a specific CaseHub definition.
     *
     * @param caseHubId the CaseHub database ID
     * @return a Uni containing the list of runs
     */
    public Uni<List<CaseHubInstanceRun>> findByCaseHubId(Long caseHubId) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "FROM CaseHubInstanceRun r WHERE r.caseHub.id = :caseHubId ORDER BY r.createdAt DESC",
                        CaseHubInstanceRun.class)
                        .setParameter("caseHubId", caseHubId)
                        .getResultList()
        );
    }

    /**
     * Find all runs with a specific status.
     *
     * @param status the status to filter by
     * @return a Uni containing the list of runs
     */
    public Uni<List<CaseHubInstanceRun>> findByStatus(CaseHubInstanceRun.CaseHubStatus status) {
        return sessionFactory.withSession(session ->
                session.createQuery("FROM CaseHubInstanceRun WHERE status = :status", CaseHubInstanceRun.class)
                        .setParameter("status", status)
                        .getResultList()
        );
    }

    /**
     * Update the status of a CaseHub instance run.
     *
     * @param runId the run ID
     * @param newStatus the new status
     * @return a Uni containing the updated run
     */
    public Uni<CaseHubInstanceRun> updateStatus(UUID runId, CaseHubInstanceRun.CaseHubStatus newStatus) {
        return sessionFactory.withTransaction((session, tx) ->
                session.createQuery("FROM CaseHubInstanceRun WHERE runId = :runId", CaseHubInstanceRun.class)
                        .setParameter("runId", runId)
                        .getSingleResultOrNull()
                        .onItem().ifNotNull().transformToUni(run -> {
                            run.setStatus(newStatus);
                            return session.merge(run);
                        })
        );
    }

    /**
     * Find run by database ID.
     *
     * @param id the database ID
     * @return a Uni containing the run, or null if not found
     */
    public Uni<CaseHubInstanceRun> findById(Long id) {
        return sessionFactory.withSession(session ->
                session.find(CaseHubInstanceRun.class, id)
        );
    }

    /**
     * Find all runs.
     *
     * @return a Uni containing all runs
     */
    public Uni<List<CaseHubInstanceRun>> findAll() {
        return sessionFactory.withSession(session ->
                session.createQuery("FROM CaseHubInstanceRun ORDER BY createdAt DESC", CaseHubInstanceRun.class)
                        .getResultList()
        );
    }
}
