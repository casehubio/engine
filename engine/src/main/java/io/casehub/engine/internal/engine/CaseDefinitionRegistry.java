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
package io.casehub.engine.internal.engine;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.Milestone;
import io.casehub.api.model.PredicateBasedCompletion;
import io.casehub.engine.internal.model.CaseMetaModel;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

/**
 * Registry for case definitions. TODO: it's a shim for now, simple becase it's not possible to
 * ser/deser CaseDefinition to yaml if it contains references to agents or java code
 */
@ApplicationScoped
public class CaseDefinitionRegistry {

  private final Map<CaseMetaModel, CaseDefinition> registry = new ConcurrentHashMap<>();

  private static final Logger LOG = Logger.getLogger(CaseDefinitionRegistry.class);

  @Inject Instance<CaseHub> caseHubInstance;

  @Inject Mutiny.SessionFactory sessionFactory;

  @Inject Vertx vertx;

  @Inject ExpressionEngineRegistry expressionEngineRegistry;

  // TODO this must be reworked
  void onStart(@Observes @Priority(10) StartupEvent ev) {
    ReactiveUtils.runOnSafeVertxContext(vertx, this::registerKnownDefinitions)
        .await()
        .atMost(Duration.ofSeconds(30));
  }

  Uni<Void> registerKnownDefinitions() {
    return sessionFactory.withSession(
        session ->
            Multi.createFrom()
                .iterable(caseHubInstance)
                .onItem()
                .transformToUniAndConcatenate(hub -> registerCaseDefinition(hub.getDefinition()))
                .collect()
                .last()
                .replaceWithVoid());
  }

  private Uni<CaseMetaModel> registerCaseDefinition(CaseDefinition model) {
    try {
      validateExpressions(model);
    } catch (IllegalArgumentException e) {
      LOG.errorf("Case definition '%s' rejected: %s", model.getName(), e.getMessage());
      return Uni.createFrom().failure(e);
    }

    LOG.info(
        "Registering case: "
            + model.getName()
            + " version: "
            + model.getVersion()
            + " namespace: "
            + model.getNamespace());

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
            model.getNamespace(),
            model.getName(),
            model.getVersion())
        .firstResult()
        .onItem()
        .transformToUni(
            existing -> {
              if (existing != null) {
                registry.put(existing, model);
                return Uni.createFrom().item(existing);
              }

              definition.setDsl(model.getDsl());
              definition.setCreatedAt(Instant.now());

              return definition
                  .persistAndFlush()
                  .replaceWith(definition)
                  .invoke(persisted -> registry.put(persisted, model));
            });
  }

  public CaseDefinition getCaseDefinition(CaseMetaModel definition) {
    return registry.get(definition);
  }

  public CaseMetaModel getCaseMetaModel(CaseDefinition caseDefinition) {
    for (Map.Entry<CaseMetaModel, CaseDefinition> entry : registry.entrySet()) {
      if (entry.getValue().equals(caseDefinition)) {
        return entry.getKey();
      }
    }
    throw new RuntimeException(
        "CaseMetaModel not found for caseDefinition: "
            + caseDefinition.getNamespace()
            + "."
            + caseDefinition.getName()
            + ":"
            + caseDefinition.getVersion());
  }

  private void validateExpressions(CaseDefinition definition) {
    if (definition.getBindings() != null) {
      for (Binding rule : definition.getBindings()) {
        if (rule.getOn() instanceof ContextChangeTrigger cct) {
          expressionEngineRegistry.validate(cct.getFilter());
        }
        expressionEngineRegistry.validate(rule.getWhen());
      }
    }
    if (definition.getMilestones() != null) {
      for (Milestone milestone : definition.getMilestones()) {
        expressionEngineRegistry.validate(milestone.getCondition());
      }
    }
    if (definition.getGoals() != null) {
      for (Goal goal : definition.getGoals()) {
        expressionEngineRegistry.validate(goal.getCondition());
      }
    }
    if (definition.getCompletion() instanceof PredicateBasedCompletion pbc) {
      expressionEngineRegistry.validate(pbc.getDoneWhen());
    }
  }
}
