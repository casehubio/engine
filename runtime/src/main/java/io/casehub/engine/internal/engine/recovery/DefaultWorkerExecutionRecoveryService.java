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

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.qualifier.CrossTenant;
import io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository;
import io.casehub.engine.common.spi.CrossTenantEventLogRepository;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.common.spi.recovery.WorkerExecutionRecoveryService;
import io.casehub.engine.common.spi.scheduler.WorkerExecutionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DefaultWorkerExecutionRecoveryService implements WorkerExecutionRecoveryService {

  private static final Logger LOG = Logger.getLogger(DefaultWorkerExecutionRecoveryService.class);

  private static final EnumSet<CaseHubEventType> RELEVANT_RECOVERY_EVENTS =
      EnumSet.of(
          CaseHubEventType.WORKER_SCHEDULED,
          CaseHubEventType.WORKER_EXECUTION_STARTED,
          CaseHubEventType.WORKER_EXECUTION_COMPLETED,
          CaseHubEventType.WORKER_EXECUTION_FAILED,
          CaseHubEventType.MILESTONE_ACTIVATED,
          CaseHubEventType.MILESTONE_COMPLETED,
          CaseHubEventType.MILESTONE_SLA_VIOLATED);

  @Inject @CrossTenant CrossTenantCaseInstanceRepository caseInstanceRepository;

  @Inject @CrossTenant CrossTenantEventLogRepository eventLogRepository;

  @Inject CaseInstanceCache caseInstanceCache;

  @Inject WorkerExecutionManager workflowExecutionManager;

  @Inject io.casehub.engine.common.spi.recovery.CaseContextRecoveryStrategy recoveryStrategy;

  @Override
  public CaseInstance loadOrRestoreCaseInstance(UUID caseId) {
    CaseInstance cached = caseInstanceCache.get(caseId);
    if (cached != null) {
      return cached;
    }

    CaseInstance instance =
        caseInstanceRepository
            .findByUuid(caseId)
            .orElseThrow(() -> new IllegalStateException("CaseInstance not found for caseId=" + caseId));
    CaseContext stateContext = recoveryStrategy.recover(instance);
    instance.setCaseContext(stateContext);
    caseInstanceCache.put(instance);
    return instance;
  }

  @Override
  public void recoverPendingScheduledWorkers() {
    List<EventLog> eventLogs = eventLogRepository.findByTypes(RELEVANT_RECOVERY_EVENTS);
    reschedulePendingEvents(eventLogs);
  }

  private void reschedulePendingEvents(List<EventLog> eventLogs) {
    Set<String> alreadyProgressed = new HashSet<>();
    for (EventLog eventLog : eventLogs) {
      if (eventLog.getEventType() != CaseHubEventType.WORKER_SCHEDULED) {
        String key = executionKey(eventLog);
        if (key != null) {
          alreadyProgressed.add(key);
        }
      }
    }

    eventLogs.stream()
        .filter(
            eventLog -> {
              if (eventLog.getEventType() != CaseHubEventType.WORKER_SCHEDULED) {
                return false;
              }
              String key = executionKey(eventLog);
              return key != null && !alreadyProgressed.contains(key);
            })
        .forEach(workflowExecutionManager::schedulePersistedEvent);
  }

  private String executionKey(EventLog eventLog) {
    com.fasterxml.jackson.databind.JsonNode metadata = eventLog.getMetadata();
    if (metadata == null || eventLog.getCaseId() == null || eventLog.getWorkerId() == null) {
      return null;
    }
    com.fasterxml.jackson.databind.JsonNode inputDataHash = metadata.get("inputDataHash");
    if (inputDataHash == null || inputDataHash.isNull()) {
      return null;
    }
    return eventLog.getCaseId() + "|" + eventLog.getWorkerId() + "|" + inputDataHash.asText();
  }
}
