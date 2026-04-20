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
import io.casehub.engine.spi.EventLogRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.shareddata.Lock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WorkerScheduleEventHandler {

  private static final Logger LOG = Logger.getLogger(WorkerScheduleEventHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject Vertx vertx;

  @Inject WorkerExecutionManager workflowExecutionManager;

  @Inject WorkerExecutionGuard workerExecutionGuard;

  @Inject EventBus eventBus;

  @Inject EventLogRepository eventLogRepository;

  @ConsumeEvent(value = EventBusAddresses.WORKER_SCHEDULE)
  public Uni<Void> onWorkerScheduleEventHandler(WorkerScheduleEvent event) {
    CaseInstance instance = event.caseInstance();
    Worker worker = event.worker();
    String inputDataHash =
        WorkerExecutionKeys.inputDataHash(
            worker.getName(),
            event.capability().getName(),
            instance.getCaseContext().evalObjectTemplate(event.capability().getInputSchema()));

    if (workerExecutionGuard.isBlocked(worker.getName(), instance.getUuid())) {
      LOG.warnf(
          "Worker blocked by guard (quarantined?): caseId=%s worker=%s — emitting retries exhausted",
          instance.getUuid(), worker.getName());
      eventBus.publish(
          EventBusAddresses.WORKER_RETRIES_EXHAUSTED,
          new WorkerRetriesExhaustedEvent(instance.getUuid(), worker.getName(), inputDataHash));
      return Uni.createFrom().voidItem();
    }
    Capability capability = event.capability();

    Map<String, Object> inputData =
        instance.getCaseContext().evalObjectTemplate(capability.getInputSchema());

    EventLog eventLog = buildEventLog(instance, worker, capability, inputData, inputDataHash);

    String lockKey = "wse:" + instance.getUuid() + ":" + worker.getName() + ":" + inputDataHash;

    return vertx
        .sharedData()
        .getLocalLock(lockKey)
        .chain(
            lock ->
                scheduleUnderLock(
                    lock, eventLog, instance, worker, capability, inputData, inputDataHash));
  }

  private Uni<Void> scheduleUnderLock(
      Lock lock,
      EventLog eventLog,
      CaseInstance instance,
      Worker worker,
      Capability capability,
      Map<String, Object> inputData,
      String inputDataHash) {
    return eventLogRepository
        .findSchedulingEvents(instance.getUuid(), worker.getName())
        .map(existing -> decideAction(existing, inputDataHash))
        .chain(action -> executeAction(action, eventLog, instance, worker, capability))
        .chain(eventLogId -> submitIfNeeded(eventLogId, instance, worker, capability, inputData))
        .invoke(
            () ->
                LOG.infof(
                    "WorkerScheduleEvent processed: caseId=%s worker=%s capability=%s",
                    instance.getUuid(), worker.getName(), capability.getName()))
        .invoke(lock::release)
        .replaceWithVoid()
        .onFailure()
        .invoke(
            t ->
                LOG.errorf(
                    t,
                    "WorkerScheduleEvent FAILED: caseId=%s worker=%s capability=%s",
                    instance.getUuid(),
                    worker.getName(),
                    capability.getName()))
        .invoke(lock::release);
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
            "Skipping WorkerScheduleEvent: already scheduled/started/completed caseId=%s worker=%s capability=%s",
            instance.getUuid(), worker.getName(), capability.getName());
        yield Uni.createFrom().nullItem();
      }
      case CREATE_NEW -> eventLogRepository.appendAndReturnId(eventLog);
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

    boolean alreadyScheduledOrStartedOrCompleted =
        sameInputEvents.stream()
            .anyMatch(
                eventLog ->
                    eventLog.getEventType() == CaseHubEventType.WORKER_SCHEDULED
                        || eventLog.getEventType() == CaseHubEventType.WORKER_EXECUTION_STARTED
                        || eventLog.getEventType() == CaseHubEventType.WORKER_EXECUTION_COMPLETED);
    if (alreadyScheduledOrStartedOrCompleted) {
      // Live duplicate schedule events must not re-submit the same Quartz job.
      // If a WORKER_SCHEDULED event was persisted but never executed due to a crash,
      // WorkerExecutionRecoveryService is responsible for replaying it.
      return ScheduleAction.skip();
    }
    return ScheduleAction.createNew();
  }

  private record ScheduleAction(ScheduleActionType type, Long eventLogId) {

    static ScheduleAction skip() {
      return new ScheduleAction(ScheduleActionType.SKIP, null);
    }

    static ScheduleAction createNew() {
      return new ScheduleAction(ScheduleActionType.CREATE_NEW, null);
    }
  }

  private enum ScheduleActionType {
    SKIP,
    CREATE_NEW
  }
}
