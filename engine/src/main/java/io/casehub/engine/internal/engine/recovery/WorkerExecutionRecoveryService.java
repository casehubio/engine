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
package io.casehub.engine.internal.engine.recovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.context.CaseContext;
import io.casehub.engine.internal.context.CaseContextImpl;
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WorkerExecutionRecoveryService {

  private static final Logger LOG = Logger.getLogger(WorkerExecutionRecoveryService.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final EnumSet<CaseHubEventType> RELEVANT_RECOVERY_EVENTS =
      EnumSet.of(
          CaseHubEventType.WORKER_SCHEDULED,
          CaseHubEventType.WORKER_EXECUTION_STARTED,
          CaseHubEventType.WORKER_EXECUTION_COMPLETED,
          CaseHubEventType.WORKER_EXECUTION_FAILED);

  @Inject Mutiny.SessionFactory sessionFactory; // TODO fix it

  @Inject Vertx vertx;

  @Inject CaseInstanceCache caseInstanceCache;

  @Inject WorkerExecutionManager workflowExecutionManager;

  public Uni<CaseInstance> loadOrRestoreCaseInstance(UUID caseId) {
    CaseInstance cached = caseInstanceCache.get(caseId);
    if (cached != null) {
      return Uni.createFrom().item(cached);
    }

    // TODO not really great
    return runOnSafeContext(
            () ->
                sessionFactory.withSession(
                    session ->
                        session
                            .createSelectionQuery(
                                "from case_instance ci join fetch ci.caseMetaModel where ci.uuid = :uuid",
                                CaseInstance.class)
                            .setParameter("uuid", caseId)
                            .getSingleResultOrNull()))
        .onItem()
        .ifNull()
        .failWith(() -> new IllegalStateException("CaseInstance not found for caseId=" + caseId))
        .chain(
            instance ->
                rebuildStateContext(caseId)
                    .map(
                        stateContext -> {
                          instance.setCaseContext(stateContext);
                          caseInstanceCache.put(instance);
                          return instance;
                        }));
  }

  public Uni<Void> recoverPendingScheduledWorkers() {
    return runOnSafeContext(
            () ->
                sessionFactory.withSession(
                    session ->
                        session
                            .createSelectionQuery(
                                "from EventLog where eventType in :eventTypes order by seq asc",
                                EventLog.class)
                            .setParameter("eventTypes", RELEVANT_RECOVERY_EVENTS)
                            .getResultList()))
        .chain(this::reschedulePendingEvents);
  }

  private Uni<Void> reschedulePendingEvents(List<EventLog> eventLogs) {
    // First pass: collect keys for every execution that has already been started,
    // completed, or failed. Events are ordered by seq asc, so WORKER_SCHEDULED always
    // appears before its matching terminal event in the stream. A single-pass approach
    // would include every SCHEDULED event for recovery because the terminal event hasn't
    // been seen yet when the SCHEDULED event is processed.
    Set<String> alreadyProgressed = new HashSet<>();
    for (EventLog eventLog : eventLogs) {
      if (eventLog.getEventType() != CaseHubEventType.WORKER_SCHEDULED) {
        String key = executionKey(eventLog);
        if (key != null) {
          alreadyProgressed.add(key);
        }
      }
    }

    // Second pass: only reschedule WORKER_SCHEDULED events that have no matching
    // STARTED / COMPLETED / FAILED record (i.e. genuinely interrupted mid-flight).
    List<Uni<Void>> recoveries =
        eventLogs.stream()
            .filter(
                eventLog -> {
                  if (eventLog.getEventType() != CaseHubEventType.WORKER_SCHEDULED) {
                    return false;
                  }
                  String key = executionKey(eventLog);
                  return key != null && !alreadyProgressed.contains(key);
                })
            .map(workflowExecutionManager::schedulePersistedEvent)
            .toList();

    if (recoveries.isEmpty()) {
      return Uni.createFrom().voidItem();
    }

    return Uni.combine().all().unis(recoveries).discardItems();
  }

  @SuppressWarnings("unchecked")
  private Uni<CaseContext> rebuildStateContext(UUID caseId) {
    return runOnSafeContext(
            () ->
                sessionFactory.withSession(
                    session ->
                        session
                            .createSelectionQuery(
                                "from EventLog where caseId = :caseId and eventType in :eventTypes order by seq asc",
                                EventLog.class)
                            .setParameter("caseId", caseId)
                            .setParameter(
                                "eventTypes",
                                EnumSet.of(
                                    CaseHubEventType.CASE_STARTED,
                                    CaseHubEventType.WORKER_EXECUTION_COMPLETED,
                                    CaseHubEventType.SIGNAL_RECEIVED))
                            .getResultList()))
        .map(
            eventLogs -> {
              CaseContext caseContext = new CaseContextImpl();
              EventLog caseStartedEvent =
                  eventLogs.stream()
                      .filter(eventLog -> eventLog.getEventType() == CaseHubEventType.CASE_STARTED)
                      .findFirst()
                      .orElse(null);

              if (caseStartedEvent != null) {
                caseContext = new CaseContextImpl(payloadAsMap(caseStartedEvent.getPayload()));
              }

              for (EventLog eventLog : eventLogs) {
                if (eventLog.getEventType() == CaseHubEventType.CASE_STARTED) {
                  continue;
                }

                if (eventLog.getEventType() == CaseHubEventType.SIGNAL_RECEIVED) {
                  JsonNode patch = payloadAsPatch(eventLog.getPayload());
                  if (patch != null) {
                    caseContext.applyDiff(patch);
                  }
                } else if (eventLog.getEventType() == CaseHubEventType.WORKER_EXECUTION_COMPLETED) {
                  caseContext.setAll(payloadAsMap(eventLog.getPayload()));
                } else {
                  LOG.warnf(
                      "Unexpected event type in rebuildStateContext: %s", eventLog.getEventType());
                }
              }
              return caseContext;
            });
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> payloadAsMap(JsonNode payload) {
    return OBJECT_MAPPER.convertValue(
        payload == null ? OBJECT_MAPPER.createObjectNode() : payload, Map.class);
  }

  private JsonNode payloadAsPatch(JsonNode payload) {
    if (payload == null || payload.isNull()) {
      return null;
    }
    JsonNode patch = payload.get("patch");
    return patch != null && patch.isArray() ? patch : null;
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
