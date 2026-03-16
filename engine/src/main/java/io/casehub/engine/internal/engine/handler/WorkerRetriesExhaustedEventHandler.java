package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.CaseStatusChanged;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerRetriesExhaustedEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseState;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

@ApplicationScoped
public class WorkerRetriesExhaustedEventHandler {

    private static final Logger LOG = Logger.getLogger(WorkerRetriesExhaustedEventHandler.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    CaseInstanceCache caseInstanceCache;

    @Inject
    EventBus eventBus;

    @ConsumeEvent(value = EventBusAddresses.WORKER_RETRIES_EXHAUSTED)
    public Uni<Void> onWorkerRetriesExhaustedEvent(WorkerRetriesExhaustedEvent event) {
        CaseInstance caseInstance = caseInstanceCache.get(event.caseId());
        String oldStatus = caseInstance.getState().name();
        caseInstance.setState(CaseState.FAILED);

        EventLog eventLog = new EventLog();
        eventLog.setEventType(CaseHubEventType.CASE_FAILED);
        eventLog.setCaseId(caseInstance.getUuid());
        eventLog.setStreamType(EventStreamType.CASE);
        eventLog.setTimestamp(Instant.now());
        eventLog.setWorkerId(event.workerId());
        eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
                .put("workerId", event.workerId())
                .put("idempotency", event.idempotency()));

        return Panache.withTransaction(() ->
                        caseInstance.persist()
                                .chain(eventLog::persist))
                .invoke(() -> {
                    LOG.warnf("Worker retries exhausted for caseId=%s, workerId=%s", event.caseId(), event.workerId());
                    eventBus.publish(EventBusAddresses.CASE_STATUS_CHANGED,
                            new CaseStatusChanged(caseInstance, oldStatus, CaseState.FAILED.name()));
                })
                .replaceWithVoid();
    }
}
