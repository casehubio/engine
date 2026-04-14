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
package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.casehub.api.spi.WorkerExecutionGuard;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerRetriesExhaustedEvent;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.WorkerExecutionKeys;
import io.casehub.engine.internal.worker.WorkerExecutionManager;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WorkerScheduleEventHandler {

  private static final Logger LOG = Logger.getLogger(WorkerScheduleEventHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject WorkerExecutionManager workflowExecutionManager;

  @Inject WorkerExecutionGuard workerExecutionGuard;

  @Inject io.vertx.mutiny.core.eventbus.EventBus eventBus;

  @ConsumeEvent(value = EventBusAddresses.WORKER_SCHEDULE)
  public Uni<Void> onWorkerScheduleEventHandler(WorkerScheduleEvent event) {
    CaseInstance instance = event.caseInstance();
    Worker worker = event.worker();

    if (workerExecutionGuard.isBlocked(worker.getName(), instance.getUuid())) {
      LOG.warnf(
          "Worker blocked by guard (quarantined?): caseId=%s worker=%s — emitting retries exhausted",
          instance.getUuid(), worker.getName());
      String idempotency =
          io.casehub.engine.internal.util.WorkerExecutionKeys.inputDataHash(
              worker.getName(),
              event.capability().getName(),
              instance.getCaseContext().evalObjectTemplate(event.capability().getInputSchema()));
      eventBus.publish(
          EventBusAddresses.WORKER_RETRIES_EXHAUSTED,
          new WorkerRetriesExhaustedEvent(instance.getUuid(), worker.getName(), idempotency));
      return Uni.createFrom().voidItem();
    }
    Capability capability = event.capability();

    Map<String, Object> inputData =
        instance.getCaseContext().evalObjectTemplate(capability.getInputSchema());
    String inputDataHash =
        WorkerExecutionKeys.inputDataHash(worker.getName(), capability.getName(), inputData);
    EventLog eventLog = buildEventLog(instance, worker, capability, inputData, inputDataHash);

    return Panache.withTransaction(
            () ->
                EventLog.findSchedulingEvents(instance.getUuid(), worker.getName())
                    .map(existing -> decideAction(existing, inputDataHash))
                    .chain(action -> executeAction(action, eventLog, instance, worker, capability)))
        .chain(eventLogId -> submitIfNeeded(eventLogId, instance, worker, capability, inputData))
        .invoke(
            () ->
                LOG.infof(
                    "WorkerScheduleEvent processed: caseId=%s worker=%s capability=%s",
                    instance.getUuid(), worker.getName(), capability.getName()))
        .replaceWithVoid()
        .onFailure()
        .invoke(
            t ->
                LOG.errorf(
                    t,
                    "WorkerScheduleEvent FAILED: caseId=%s worker=%s capability=%s",
                    instance.getUuid(),
                    worker.getName(),
                    capability.getName()));
  }

  private EventLog buildEventLog(
      CaseInstance instance,
      Worker worker,
      Capability capability,
      Map<String, Object> inputData,
      String inputDataHash) {
    Map<String, String> metadata =
        Map.of(
            "workerName", worker.getName(),
            "capabilityName", capability.getName(),
            "inputDataHash", inputDataHash);

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(instance.getUuid());
    eventLog.setEventType(CaseHubEventType.WORKER_SCHEDULED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setWorkerId(worker.getName());
    eventLog.setMetadata(OBJECT_MAPPER.valueToTree(metadata));
    eventLog.setPayload(OBJECT_MAPPER.valueToTree(inputData));
    return eventLog;
  }

  private Uni<Long> executeAction(
      ScheduleAction action,
      EventLog eventLog,
      CaseInstance instance,
      Worker worker,
      Capability capability) {
    return switch (action.type()) {
      case SKIP -> {
        LOG.infof(
            "Skipping WorkerScheduleEvent: already started/completed caseId=%s worker=%s capability=%s",
            instance.getUuid(), worker.getName(), capability.getName());
        yield Uni.createFrom().nullItem();
      }
      case RESUBMIT_EXISTING -> {
        LOG.infof(
            "Re-submitting existing scheduled worker: caseId=%s worker=%s capability=%s eventLogId=%d",
            instance.getUuid(), worker.getName(), capability.getName(), action.eventLogId());
        yield Uni.createFrom().item(action.eventLogId());
      }
      case CREATE_NEW -> eventLog.persistScheduledEvent();
    };
  }

  private Uni<Void> submitIfNeeded(
      Long eventLogId,
      CaseInstance instance,
      Worker worker,
      Capability capability,
      Map<String, Object> inputData) {
    if (eventLogId == null) {
      return Uni.createFrom().voidItem();
    }
    return workflowExecutionManager.submit(eventLogId, instance, worker, capability, inputData);
  }

  private ScheduleAction decideAction(List<EventLog> existingEvents, String executionIdempotency) {
    List<EventLog> sameInputEvents =
        existingEvents.stream()
            .filter(
                eventLog -> {
                  JsonNode metadata = eventLog.getMetadata();
                  JsonNode existingHash = metadata == null ? null : metadata.get("inputDataHash");
                  return existingHash != null && executionIdempotency.equals(existingHash.asText());
                })
            .toList();

    boolean alreadyStartedOrCompleted =
        sameInputEvents.stream()
            .anyMatch(
                eventLog ->
                    eventLog.getEventType() == CaseHubEventType.WORKER_EXECUTION_STARTED
                        || eventLog.getEventType() == CaseHubEventType.WORKER_EXECUTION_COMPLETED);
    if (alreadyStartedOrCompleted) {
      return ScheduleAction.skip();
    }

    Optional<Long> existingScheduledId =
        sameInputEvents.stream()
            .filter(eventLog -> eventLog.getEventType() == CaseHubEventType.WORKER_SCHEDULED)
            .max(Comparator.comparing(eventLog -> eventLog.id))
            .map(eventLog -> eventLog.id);

    return existingScheduledId
        .map(ScheduleAction::resubmitExisting)
        .orElseGet(ScheduleAction::createNew);
  }

  private record ScheduleAction(ScheduleActionType type, Long eventLogId) {

    static ScheduleAction skip() {
      return new ScheduleAction(ScheduleActionType.SKIP, null);
    }

    static ScheduleAction resubmitExisting(Long eventLogId) {
      return new ScheduleAction(ScheduleActionType.RESUBMIT_EXISTING, eventLogId);
    }

    static ScheduleAction createNew() {
      return new ScheduleAction(ScheduleActionType.CREATE_NEW, null);
    }
  }

  private enum ScheduleActionType {
    SKIP,
    RESUBMIT_EXISTING,
    CREATE_NEW
  }
}
