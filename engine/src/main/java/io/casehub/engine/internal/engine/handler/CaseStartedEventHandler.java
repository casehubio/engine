package io.casehub.engine.internal.engine.handler;

import io.casehub.context.StateContext;
import io.casehub.engine.internal.event.CaseStartedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.StateContextChangedEvent;
import io.casehub.engine.internal.model.CaseHubInstanceRun;
import io.casehub.engine.internal.model.CaseHubInstanceRunState;
import io.casehub.engine.internal.repository.CaseHubInstanceRunRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CaseStartedEventHandler {

    private static final Logger LOG = Logger.getLogger(CaseStartedEventHandler.class);

    @Inject
    CaseHubInstanceRunRepository runRepository;

    @Inject
    EventBus eventBus;

    @ConsumeEvent(value = EventBusAddresses.CASE_STARTED)
    public Uni<Void> onCaseStarted(CaseStartedEvent event) {
        LOG.infof("=== CaseStartedEvent received ===");
        LOG.infof("Run ID: %s", event.runId());

        StateContext context = event.stateContext();

        LOG.infof("StateContext received with %d keys", context.size());
        LOG.infof("Initial state: %s", context.asJsonNode().toPrettyString());

        return runRepository.findByRunId(event.runId())
            .onItem().ifNull().failWith(() -> new IllegalStateException("Run not found: " + event.runId()))
            .onItem().invoke(run -> {
                CaseHubInstanceRunState runState = new CaseHubInstanceRunState(run, context);

                StateContextChangedEvent contextChangedEvent = new StateContextChangedEvent(
                    runState,
                    "INITIALIZED"
                );
                eventBus.publish(EventBusAddresses.CONTEXT_CHANGED, contextChangedEvent);
                LOG.infof("Published StateContextChangedEvent for runId: %s (type: INITIALIZED)", event.runId());
            })
            .onFailure().invoke(e -> LOG.errorf(e, "Failed to process CaseStartedEvent for runId: %s", event.runId()))
            .replaceWithVoid();
    }
}
