package io.casehub.engine.internal.engine;

import io.casehub.api.context.StateContext;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.CaseStartedEvent;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseState;
import io.casehub.api.model.CaseHubDefinition;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.casehub.engine.internal.event.EventBusAddresses.CASE_STARTED;

@ApplicationScoped
public class CaseHubReactor {

  private static final Logger LOG = Logger.getLogger(CaseHubReactor.class);

  @Inject
  CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject
  CaseInstanceCache caseInstanceCache;

  @Inject
  Mutiny.SessionFactory sessionFactory;

  @Inject
  EventBus eventBus;

  public CompletionStage<UUID> startCase(CaseHubDefinition definition, StateContext context) {
    return sessionFactory.withTransaction((session, tx) ->
                    caseDefinitionRegistry
                            .registerCaseDefinition(definition)
                            .onItem().transformToUni(def -> {
                              UUID caseId = UUID.randomUUID();

                              CaseInstance instance = new CaseInstance();
                              instance.setUuid(caseId);
                              instance.setCaseDefinition(def);
                              instance.setVersion(0L);
                              instance.setState(CaseState.ACTIVE);
                              instance.setStateContext(context);

                              caseInstanceCache.put(instance);

                              return instance.<CaseInstance>persist();
                            })
            ).invoke(instance -> {
              LOG.info("Case started with caseId: " + instance.getUuid());
              eventBus.publish(CASE_STARTED, new CaseStartedEvent(instance));
            })
            .onItem()
            .transform(CaseInstance::getUuid)
            .subscribeAsCompletionStage();
  }

  public CompletionStage<Object> query(UUID caseId, String path) {
    return CompletableFuture.supplyAsync(() -> {
      if(caseInstanceCache.get(caseId) == null) {
        throw new RuntimeException("Case instance not found for caseId: " + caseId);
      }
      return caseInstanceCache.get(caseId).getStateContext().getPath(path);
    });
  }

  @SuppressWarnings("unchecked")
  public <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz) {
    return query(caseId, path).thenApply(result -> {
      if (result == null) {
        return null;
      }
      if (clazz.isInstance(result)) {
        return clazz.cast(result);
      }
      throw new ClassCastException("Cannot cast " + result.getClass().getName() + " to " + clazz.getName());
    });
  }
}
