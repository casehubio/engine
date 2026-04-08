package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.context.StateContext;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.engine.recovery.WorkerExecutionRecoveryService;
import io.casehub.engine.internal.event.CaseStateContextChangedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.SignalReceivedEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

import static io.casehub.engine.internal.event.EventBusAddresses.CONTEXT_CHANGED;

@ApplicationScoped
public class SignalReceivedEventHandler {

    private static final Logger LOG = Logger.getLogger(SignalReceivedEventHandler.class);

    @Inject
    EventBus eventBus;

    @Inject
    CaseInstanceCache caseInstanceCache;

    @Inject
    WorkerExecutionRecoveryService recoveryService;

    @ConsumeEvent(value = EventBusAddresses.SIGNAL_RECEIVED)
    public Uni<Void> onSignalReceived(SignalReceivedEvent event) {
        CaseInstance cached = caseInstanceCache.get(event.caseId());
        if (cached != null) {
            return applySignal(cached, event);
        }
        LOG.warnf("CaseInstance not found in cache for caseId=%s, trying recovery", event.caseId());
        return recoveryService.loadOrRestoreCaseInstance(event.caseId())
                .chain(instance -> applySignal(instance, event));
    }

    private Uni<Void> applySignal(CaseInstance instance, SignalReceivedEvent event) {
        StateContext before = instance.getStateContext().snapshot();
        instance.getStateContext().setPath(event.path(), event.value());
        JsonNode diff = before.diff(instance.getStateContext());

        return Panache.withTransaction(() -> {
                    EventLog eventLog = buildSignalEventLog(instance, diff);
                    return eventLog.persist()
                            .invoke(() -> eventBus.publish(CONTEXT_CHANGED, new CaseStateContextChangedEvent(instance)));
                })
                .replaceWithVoid()
                .onFailure().invoke(t -> LOG.errorf(t, "Failed to process signal path='%s' for caseId=%s", event.path(), event.caseId()));
    }

    private EventLog buildSignalEventLog(CaseInstance instance, JsonNode diff) {
        EventLog eventLog = new EventLog();
        eventLog.setCaseId(instance.getUuid());
        eventLog.setEventType(CaseHubEventType.SIGNAL_RECEIVED);
        eventLog.setStreamType(EventStreamType.CASE);
        eventLog.setTimestamp(Instant.now());
        eventLog.setPayload(diff);
        return eventLog;
    }
}
