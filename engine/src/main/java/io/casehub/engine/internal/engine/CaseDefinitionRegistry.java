package io.casehub.engine.internal.engine;

import io.casehub.engine.internal.model.CaseDefinition;
import io.casehub.api.model.CaseHubDefinition;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

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

  private final Map<CaseDefinition, CaseHubDefinition> registry = new ConcurrentHashMap<>();

  Uni<CaseDefinition> registerCaseDefinition(CaseHubDefinition model) {
    CaseDefinition definition = new CaseDefinition();
    definition.setName(model.getName());
    definition.setNamespace(model.getNamespace());
    definition.setVersion(model.getVersion());

    for (CaseDefinition registered : registry.keySet()) {
      if (registered.equals(definition)) {
        return Uni.createFrom().item(registered);
      }
    }

    return CaseDefinition.find(
                    "namespace = ?1 and name = ?2 and version = ?3",
                    model.getNamespace(), model.getName(), model.getVersion()
            ).firstResult()
            .onItem().transformToUni(existing -> {
              if (existing != null) {
                registry.put(definition, model);
                return Uni.createFrom().voidItem();
              }
              definition.setDsl(model.getDsl());
              definition.setCreatedAt(Instant.now());

              return definition.persist()
                      .invoke(() -> registry.put(definition, model));
            }).replaceWith(definition);
  }

  public CaseHubDefinition getCaseDefinition(CaseDefinition definition) {
    return registry.get(definition);
  }

}
