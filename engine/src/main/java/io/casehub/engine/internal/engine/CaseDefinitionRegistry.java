package io.casehub.engine.internal.engine;

import io.casehub.api.engine.CaseHub;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.engine.internal.util.ReactiveUtils;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for case definitions.
 * TODO: it's a shim for now, simple becase it's not possible to ser/deser CaseHubDefinition to yaml if it
 * contains references to agents  or java code
 */
@ApplicationScoped
public class CaseDefinitionRegistry {

    private final Map<CaseMetaModel, CaseHubDefinition> registry = new ConcurrentHashMap<>();

    private static final Logger LOG = Logger.getLogger(CaseDefinitionRegistry.class);

    @Inject
    Instance<CaseHub> caseHubInstance;

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Vertx vertx;

    //TODO this must be reworked
    void onStart(@Observes @Priority(10) StartupEvent ev) {
        ReactiveUtils.runOnSafeVertxContext(vertx, this::registerKnownDefinitions)
                .await().atMost(Duration.ofSeconds(30));
    }

    Uni<Void> registerKnownDefinitions() {
        return sessionFactory.withSession(session ->
                Multi.createFrom().iterable(caseHubInstance)
                        .onItem().transformToUniAndConcatenate(hub ->
                                registerCaseDefinition(hub.getDefinition())
                        )
                        .collect().last()
                        .replaceWithVoid()
        );
    }


    private Uni<CaseMetaModel> registerCaseDefinition(CaseHubDefinition model) {
        LOG.info("Registering case: " + model.getName() + " version: " + model.getVersion() + " namespace: " + model.getNamespace());

        CaseMetaModel definition = new CaseMetaModel();
        definition.setName(model.getName());
        definition.setNamespace(model.getNamespace());
        definition.setVersion(model.getVersion());

        for (CaseMetaModel registered : registry.keySet()) {
            if (registered.equals(definition)) {
                return Uni.createFrom().item(registered);
            }
        }

        return CaseMetaModel.<CaseMetaModel>find(
                        "namespace = ?1 and name = ?2 and version = ?3",
                        model.getNamespace(), model.getName(), model.getVersion()
                ).firstResult()
                .onItem()
                .transformToUni(existing -> {
                    if (existing != null) {
                        registry.put(existing, model);
                        return Uni.createFrom().item(existing);
                    }

                    definition.setDsl(model.getDsl());
                    definition.setCreatedAt(Instant.now());

                    return definition.persistAndFlush()
                            .replaceWith(definition)
                            .invoke(persisted -> registry.put(persisted, model));
                });
    }

    public CaseHubDefinition getCaseDefinition(CaseMetaModel definition) {
        return registry.get(definition);
    }

    public CaseMetaModel getCaseMetaModel(CaseHubDefinition caseDefinition) {
        for (Map.Entry<CaseMetaModel, CaseHubDefinition> entry : registry.entrySet()) {
            if (entry.getValue().equals(caseDefinition)) {
                return entry.getKey();
            }
        }
        throw new RuntimeException("CaseMetaModel not found for caseDefinition: " + caseDefinition.getNamespace() + "." + caseDefinition.getName() + ":" + caseDefinition.getVersion());
    }

}
