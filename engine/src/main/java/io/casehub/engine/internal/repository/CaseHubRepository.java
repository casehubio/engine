package io.casehub.engine.internal.repository;

import io.casehub.engine.internal.model.CaseHub;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;

/**
 * Repository for managing CaseHub definitions using Hibernate Reactive.
 */
@ApplicationScoped
public class CaseHubRepository {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    /**
     * Persist a new CaseHub definition.
     *
     * @param caseHub the CaseHub to persist
     * @return a Uni containing the persisted CaseHub
     */
    public Uni<CaseHub> persist(CaseHub caseHub) {
        return sessionFactory.withTransaction((session, tx) ->
                session.persist(caseHub)
                        .replaceWith(caseHub)
        );
    }

    /**
     * Update an existing CaseHub definition.
     *
     * @param caseHub the CaseHub to update
     * @return a Uni containing the updated CaseHub
     */
    public Uni<CaseHub> update(CaseHub caseHub) {
        return sessionFactory.withTransaction((session, tx) ->
                session.merge(caseHub)
        );
    }

    /**
     * Find a CaseHub by its database ID.
     *
     * @param id the database ID
     * @return a Uni containing the CaseHub, or null if not found
     */
    public Uni<CaseHub> findById(Long id) {
        return sessionFactory.withSession(session ->
                session.find(CaseHub.class, id)
        );
    }

    /**
     * Find a CaseHub by namespace, name, and version.
     *
     * @param namespace the namespace
     * @param name the name
     * @param version the version
     * @return a Uni containing the CaseHub, or null if not found
     */
    public Uni<CaseHub> findByNamespaceNameVersion(String namespace, String name, String version) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "FROM CaseHub WHERE namespace = :namespace AND name = :name AND version = :version",
                        CaseHub.class)
                        .setParameter("namespace", namespace)
                        .setParameter("name", name)
                        .setParameter("version", version)
                        .getSingleResultOrNull()
        );
    }

    /**
     * Find all CaseHubs with a specific namespace and name (all versions).
     *
     * @param namespace the namespace
     * @param name the name
     * @return a Uni containing the list of CaseHubs
     */
    public Uni<List<CaseHub>> findByNamespaceAndName(String namespace, String name) {
        return sessionFactory.withSession(session ->
                session.createQuery(
                        "FROM CaseHub WHERE namespace = :namespace AND name = :name ORDER BY version DESC",
                        CaseHub.class)
                        .setParameter("namespace", namespace)
                        .setParameter("name", name)
                        .getResultList()
        );
    }

    /**
     * Find all CaseHubs in a namespace.
     *
     * @param namespace the namespace
     * @return a Uni containing the list of CaseHubs
     */
    public Uni<List<CaseHub>> findByNamespace(String namespace) {
        return sessionFactory.withSession(session ->
                session.createQuery("FROM CaseHub WHERE namespace = :namespace", CaseHub.class)
                        .setParameter("namespace", namespace)
                        .getResultList()
        );
    }

    /**
     * Find all CaseHubs.
     *
     * @return a Uni containing all CaseHubs
     */
    public Uni<List<CaseHub>> findAll() {
        return sessionFactory.withSession(session ->
                session.createQuery("FROM CaseHub", CaseHub.class)
                        .getResultList()
        );
    }

}
