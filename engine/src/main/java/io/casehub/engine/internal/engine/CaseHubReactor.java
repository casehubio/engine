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

import static io.casehub.engine.internal.event.EventBusAddresses.CASE_STARTED;
import static io.casehub.engine.internal.event.EventBusAddresses.SIGNAL_RECEIVED;

import io.casehub.api.context.StateContext;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.CaseStartedEvent;
import io.casehub.engine.internal.event.SignalReceivedEvent;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

@ApplicationScoped
class CaseHubReactor {

  private static final Logger LOG = Logger.getLogger(CaseHubReactor.class);

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject CaseInstanceCache caseInstanceCache;

  @Inject Mutiny.SessionFactory sessionFactory;

  @Inject EventBus eventBus;

  CompletionStage<UUID> startCase(CaseDefinition definition, StateContext context) {
    return getCaseInstance(definition, context)
        .invoke(
            instance -> {
              LOG.info("Case started with caseId: " + instance.getUuid());
              eventBus.publish(CASE_STARTED, new CaseStartedEvent(instance));
            })
        .onItem()
        .transform(CaseInstance::getUuid)
        .subscribeAsCompletionStage();
  }

  private Uni<CaseInstance> getCaseInstance(CaseDefinition definition, StateContext context) {
    CaseMetaModel model = caseDefinitionRegistry.getCaseMetaModel(definition);

    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());
    instance.setCaseMetaModel(model);
    instance.setVersion(0L);
    instance.setState(CaseStatus.RUNNING);
    instance.setStateContext(context);

    caseInstanceCache.put(instance);
    return sessionFactory.withTransaction(session -> instance.persist());
  }

  void signal(UUID caseId, String path, Object value) {
    eventBus.publish(SIGNAL_RECEIVED, new SignalReceivedEvent(caseId, path, value));
  }

  CompletionStage<Object> query(UUID caseId, String path) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (caseInstanceCache.get(caseId) == null) {
            throw new RuntimeException("Case instance not found for caseId: " + caseId);
          }
          return caseInstanceCache.get(caseId).getStateContext().getPath(path);
        });
  }

  @SuppressWarnings("unchecked")
  <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz) {
    return query(caseId, path)
        .thenApply(
            result -> {
              if (result == null) {
                return null;
              }
              if (clazz.isInstance(result)) {
                return clazz.cast(result);
              }
              throw new ClassCastException(
                  "Cannot cast " + result.getClass().getName() + " to " + clazz.getName());
            });
  }
}
