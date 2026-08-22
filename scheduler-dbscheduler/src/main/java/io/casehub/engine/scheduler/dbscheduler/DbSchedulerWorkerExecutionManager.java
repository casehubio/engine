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
package io.casehub.engine.scheduler.dbscheduler;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import io.casehub.engine.common.internal.executor.WorkerFunctionHandler;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.utils.WorkerExecutionKeys;
import io.casehub.engine.common.qualifier.CrossTenant;
import io.casehub.engine.common.spi.CrossTenantEventLogRepository;
import io.casehub.engine.common.spi.scheduler.WorkerBackend;
import io.casehub.engine.common.spi.scheduler.WorkerExecutionManager;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jboss.logging.Logger;

@WorkerBackend
@Priority(10)
@ApplicationScoped
public class DbSchedulerWorkerExecutionManager implements WorkerExecutionManager {

  private static final Logger LOG = Logger.getLogger(DbSchedulerWorkerExecutionManager.class);

  private final ConcurrentHashMap<String, CopyOnWriteArraySet<UUID>> activeWork =
      new ConcurrentHashMap<>();

  @Inject DbSchedulerLifecycle lifecycle;
  @Inject @CrossTenant CrossTenantEventLogRepository eventLogRepository;
  @Inject Instance<WorkerFunctionHandler> functionHandlers;

  @Override
  public boolean supports(String capabilityName, String tenancyId) {
    return true;
  }

  @Override
  public boolean canExecute(WorkerFunction function) {
    for (WorkerFunctionHandler handler : functionHandlers) {
      if (handler.supports(function)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void submit(
      Long eventLogId,
      CaseInstance instance,
      Worker worker,
      Capability capability,
      Map<String, Object> inputData) {
    submit(eventLogId, instance, worker, capability, inputData, null);
  }

  @Override
  public void submit(
      Long eventLogId,
      CaseInstance instance,
      Worker worker,
      Capability capability,
      Map<String, Object> inputData,
      String bindingName) {

    String idempotency =
        WorkerExecutionKeys.inputDataHash(
            instance.getUuid(), worker.name(), capability.name(), inputData);

    WorkerTaskData taskData =
        new WorkerTaskData(
            eventLogId,
            idempotency,
            instance.getUuid(),
            worker.name(),
            instance.tenancyId,
            bindingName,
            null);

    Scheduler scheduler = lifecycle.getScheduler();
    OneTimeTask<ScheduledJobData> task =
        lifecycle.findTask(DbSchedulerLifecycle.TASK_WORKER_EXECUTION);

    String instanceId = instance.getUuid() + ":" + idempotency;
    ScheduledJobData jobData = ScheduledJobData.forWorkerExecution(taskData);

    scheduler.schedule(task.instance(instanceId, jobData), Instant.now());
    LOG.debugf("Submitted worker execution: worker=%s case=%s", worker.name(), instance.getUuid());
  }

  @Override
  public boolean supportsRecovery() {
    return true;
  }

  @Override
  public void schedulePersistedEvent(EventLog scheduledEventLog) {
    String idempotency = scheduledEventLog.getMetadata().get("inputDataHash").asText();

    WorkerTaskData taskData =
        new WorkerTaskData(
            scheduledEventLog.id,
            idempotency,
            scheduledEventLog.getCaseId(),
            scheduledEventLog.getWorkerId(),
            scheduledEventLog.tenancyId,
            null,
            null);

    Scheduler scheduler = lifecycle.getScheduler();
    OneTimeTask<ScheduledJobData> task =
        lifecycle.findTask(DbSchedulerLifecycle.TASK_WORKER_EXECUTION);

    String instanceId = scheduledEventLog.getCaseId() + ":" + idempotency;
    ScheduledJobData jobData = ScheduledJobData.forWorkerExecution(taskData);

    scheduler.schedule(task.instance(instanceId, jobData), Instant.now());
  }

  @Override
  public int getActiveWorkCount(String workerId) {
    CopyOnWriteArraySet<UUID> cases = activeWork.get(workerId);
    return cases != null ? cases.size() : 0;
  }

  @Override
  public List<UUID> getActiveCaseIds(String workerId) {
    CopyOnWriteArraySet<UUID> cases = activeWork.get(workerId);
    return cases != null ? List.copyOf(cases) : List.of();
  }

  void trackStart(String workerId, UUID caseId) {
    activeWork.computeIfAbsent(workerId, k -> new CopyOnWriteArraySet<>()).add(caseId);
  }

  void trackComplete(String workerId, UUID caseId) {
    CopyOnWriteArraySet<UUID> cases = activeWork.get(workerId);
    if (cases != null) {
      cases.remove(caseId);
    }
  }
}
