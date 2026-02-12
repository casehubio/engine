package io.casehub.engine.internal.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.context.StateContext;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.engine.internal.event.CaseStartedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.model.CaseHub;
import io.casehub.engine.internal.model.CaseHubInstanceRun;
import io.casehub.engine.internal.repository.CaseHubInstanceRunRepository;
import io.casehub.engine.internal.repository.CaseHubRepository;
import io.casehub.model.CaseHubDefinition;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class CaseHubReactor {

  private static final Logger LOG = Logger.getLogger(CaseHubReactor.class);

  @Inject
  ObjectMapper objectMapper;

  @Inject
  CaseHubRepository caseHubRepository;

  @Inject
  CaseHubInstanceRunRepository runRepository;

  @Inject
  CaseHubDefinitionRegistry definitionRegistry; // TODO since we can't ser/deser workflows made programmatically atm

  @Inject
  EventBus eventBus;

  public CaseHubReactor() {

  }

  public CompletionStage<Void> startCase(StateContext context, UUID runId) {
    LOG.infof("Starting case with runId: %s", runId);

    return runRepository.findByRunId(runId)
            .onItem().ifNull().failWith(() -> new IllegalArgumentException("CaseHub run not found: " + runId))
            .onItem().ifNotNull().transformToUni(run -> {
              LOG.infof("Found run, updating status to RUNNING");
              run.setStatus(CaseHubInstanceRun.CaseHubStatus.RUNNING);
              return runRepository.update(run);
            })
            .onItem()
            .invoke(updatedRun -> {
              CaseStartedEvent event = new CaseStartedEvent(updatedRun.getRunId(), context);

              LOG.infof("Publishing CaseStartedEvent for runId: %s", event.runId());

              eventBus.publish(EventBusAddresses.CASE_STARTED, event);
            })
            .replaceWithVoid()
            .subscribeAsCompletionStage();
  }

  public CompletionStage<Void> startCase(UUID runId) {
    return startCase(new StateContextImpl(), runId);
  }

  public CompletionStage<UUID> submitCase(CaseHubDefinition definition) {
    LOG.infof("Submitting CaseHub: %s/%s version %s",
            definition.getNamespace(), definition.getName(), definition.getVersion());

    return caseHubRepository.findByNamespaceNameVersion(
                    definition.getNamespace(),
                    definition.getName(),
                    definition.getVersion())
            .onItem().ifNull().switchTo(() -> {
              LOG.infof("Creating new CaseHub definition: %s/%s:%s",
                      definition.getNamespace(), definition.getName(), definition.getVersion());

              CaseHub caseHub = new CaseHub();
              caseHub.setNamespace(definition.getNamespace());
              caseHub.setName(definition.getName());
              caseHub.setVersion(definition.getVersion());
              caseHub.setTitle(definition.getTitle());
              caseHub.setDsl(definition.getDsl());


              // TODO since we can't ser/deser worflows made programically, we need to store the full definition in the registry
              definitionRegistry.registerDefinition(caseHub, definition);

              try {
                caseHub.setDefinition(objectMapper.valueToTree(definition));
              } catch (Exception e) {
                throw new RuntimeException("Failed to serialize CaseHubDefinition", e);
              }

              return caseHubRepository.persist(caseHub);
            })
            .onItem().transformToUni(caseHub -> {
              LOG.infof("Creating new run for CaseHub: %s/%s:%s",
                      caseHub.getNamespace(), caseHub.getName(), caseHub.getVersion());

              CaseHubInstanceRun run = new CaseHubInstanceRun();
              run.setCaseHub(caseHub);

              UUID runId = run.getRunId();
              return runRepository.persist(run)
                      .map(v -> runId);
            })
            .subscribeAsCompletionStage();
  }
}
