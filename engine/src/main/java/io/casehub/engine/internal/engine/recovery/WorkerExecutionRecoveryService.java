package io.casehub.engine.internal.engine.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.context.StateContext;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.ReactiveUtils;
import io.casehub.engine.internal.worker.WorkerExecutionManager;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class WorkerExecutionRecoveryService {

    private static final Logger LOG = Logger.getLogger(WorkerExecutionRecoveryService.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final EnumSet<CaseHubEventType> RELEVANT_RECOVERY_EVENTS = EnumSet.of(
            CaseHubEventType.WORKER_SCHEDULED,
            CaseHubEventType.WORKER_EXECUTION_STARTED,
            CaseHubEventType.WORKER_EXECUTION_COMPLETED,
            CaseHubEventType.WORKER_EXECUTION_FAILED
    );

    @Inject
    Mutiny.SessionFactory sessionFactory; //TODO fix it

    @Inject
    Vertx vertx;

    @Inject
    CaseInstanceCache caseInstanceCache;

    @Inject
    WorkerExecutionManager workflowExecutionManager;

    public Uni<CaseInstance> loadOrRestoreCaseInstance(UUID caseId) {
        CaseInstance cached = caseInstanceCache.get(caseId);
        if (cached != null) {
            return Uni.createFrom().item(cached);
        }

        // TODO not really great
        return runOnSafeContext(() ->
                sessionFactory.withSession(session ->
                        session.createSelectionQuery(
                                        "from case_instance ci join fetch ci.caseMetaModel where ci.uuid = :uuid",
                                        CaseInstance.class)
                                .setParameter("uuid", caseId)
                                .getSingleResultOrNull()
                )
        ).onItem().ifNull().failWith(() ->
                new IllegalStateException("CaseInstance not found for caseId=" + caseId)
        ).chain(instance ->
                rebuildStateContext(caseId).map(stateContext -> {
                    instance.setStateContext(stateContext);
                    caseInstanceCache.put(instance);
                    return instance;
                })
        );
    }

    public Uni<Void> recoverPendingScheduledWorkers() {
        return runOnSafeContext(() ->
                sessionFactory.withSession(session ->
                        session.createSelectionQuery(
                                        "from EventLog where eventType in :eventTypes order by seq asc",
                                        EventLog.class)
                                .setParameter("eventTypes", RELEVANT_RECOVERY_EVENTS)
                                .getResultList()
                )
        ).chain(this::reschedulePendingEvents);
    }

    private Uni<Void> reschedulePendingEvents(List<EventLog> eventLogs) {
        Set<String> alreadyStarted = new HashSet<>();
        List<Uni<Void>> recoveries = eventLogs.stream()
                .filter(eventLog -> {
                    String executionKey = executionKey(eventLog);
                    if (executionKey == null) {
                        return false;
                    }

                    if (eventLog.getEventType() == CaseHubEventType.WORKER_SCHEDULED) {
                        return !alreadyStarted.contains(executionKey);
                    }

                    alreadyStarted.add(executionKey);
                    return false;
                })
                .map(workflowExecutionManager::schedulePersistedEvent)
                .toList();

        if (recoveries.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return Uni.combine().all().unis(recoveries).discardItems();
    }

    private Uni<StateContext> rebuildStateContext(UUID caseId) {
        return runOnSafeContext(() ->
                sessionFactory.withSession(session ->
                        session.createSelectionQuery(
                                        "from EventLog where caseId = :caseId and eventType in :eventTypes order by seq asc",
                                        EventLog.class)
                                .setParameter("caseId", caseId)
                                .setParameter("eventTypes", EnumSet.of(
                                        CaseHubEventType.CASE_STARTED,
                                        CaseHubEventType.WORKER_EXECUTION_COMPLETED,
                                        CaseHubEventType.SIGNAL_RECEIVED
                                ))
                                .getResultList()
                )
        ).map(eventLogs -> {
            StateContext stateContext = new StateContextImpl();
            for (EventLog eventLog : eventLogs) {
                Map<String, Object> payload = OBJECT_MAPPER.convertValue(
                        eventLog.getPayload() == null ? OBJECT_MAPPER.createObjectNode() : eventLog.getPayload(),
                        Map.class
                );

                if (eventLog.getEventType() == CaseHubEventType.CASE_STARTED) {
                    stateContext = new StateContextImpl(payload);
                } else if (eventLog.getEventType() == CaseHubEventType.SIGNAL_RECEIVED) {
                    JsonNode patch = eventLog.getPayload();
                    if (patch != null) {
                        stateContext.applyDiff(patch);
                    }
                } else if (eventLog.getEventType() == CaseHubEventType.WORKER_EXECUTION_COMPLETED) {
                    stateContext.setAll(payload);
                } else {
                    LOG.warnf("Unexpected event type in rebuildStateContext: %s", eventLog.getEventType());
                }
            }
            return stateContext;
        });
    }

    // TODO fix it
    private String executionKey(EventLog eventLog) {
        JsonNode metadata = eventLog.getMetadata();
        if (metadata == null || eventLog.getCaseId() == null || eventLog.getWorkerId() == null) {
            return null;
        }

        JsonNode inputDataHash = metadata.get("inputDataHash");
        if (inputDataHash == null || inputDataHash.isNull()) {
            return null;
        }

        return eventLog.getCaseId() + "|" + eventLog.getWorkerId() + "|" + inputDataHash.asText();
    }

    private <T> Uni<T> runOnSafeContext(java.util.function.Supplier<Uni<? extends T>> supplier) {
        return ReactiveUtils.runOnSafeVertxContext(vertx, supplier);
    }
}
