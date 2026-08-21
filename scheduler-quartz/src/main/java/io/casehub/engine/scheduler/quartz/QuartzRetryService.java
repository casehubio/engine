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
package io.casehub.engine.scheduler.quartz;

import io.casehub.engine.common.internal.executor.RetryHandler;
import io.casehub.engine.common.internal.executor.RetryOrchestrator;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
class QuartzRetryService implements RetryHandler {

  private static final Logger LOG = Logger.getLogger(QuartzRetryService.class);

  private final RetryOrchestrator retryOrchestrator;
  private final QuartzWorkerSchedulerService schedulerService;

  @Inject
  QuartzRetryService(
      RetryOrchestrator retryOrchestrator, QuartzWorkerSchedulerService schedulerService) {
    this.retryOrchestrator = retryOrchestrator;
    this.schedulerService = schedulerService;
  }

  @Override
  public void handleFailure(WorkerTaskData taskData, Throwable cause, String errorMessage) {
    retryOrchestrator.handleFailure(taskData, cause, errorMessage, this::rescheduleViaQuartz);
  }

  void handleFailure(WorkerRetryContext ctx, String errorMessage) {
    WorkerTaskData taskData =
        new WorkerTaskData(
            Long.parseLong(ctx.eventLogId()),
            ctx.inputDataHash(),
            ctx.caseId(),
            ctx.workerId(),
            ctx.tenancyId(),
            ctx.bindingName(),
            ctx.signalId());
    retryOrchestrator.handleFailure(taskData, null, errorMessage, this::rescheduleViaQuartz);
  }

  private void rescheduleViaQuartz(WorkerTaskData taskData, java.time.Duration delay) {
    String group = taskData.caseId().toString();
    org.quartz.JobKey jobKey = new org.quartz.JobKey(taskData.inputDataHash(), group);

    org.quartz.JobDataMap dataMap = new org.quartz.JobDataMap();
    dataMap.put("inputDataHash", taskData.inputDataHash());
    dataMap.put("eventLogId", String.valueOf(taskData.eventLogId()));
    dataMap.put("workerId", taskData.workerId());
    dataMap.put("caseHubInstanceUuid", taskData.caseId().toString());
    dataMap.put("tenancyId", taskData.tenancyId());
    if (taskData.bindingName() != null) {
      dataMap.put("bindingName", taskData.bindingName());
    }
    if (taskData.signalId() != null) {
      dataMap.put("signalId", taskData.signalId().toString());
    }

    org.quartz.JobDetail job =
        org.quartz.JobBuilder.newJob(QuartzWorkerExecutionJob.class)
            .withIdentity(jobKey)
            .storeDurably(false)
            .usingJobData(dataMap)
            .build();

    org.quartz.Trigger trigger =
        org.quartz.TriggerBuilder.newTrigger()
            .withIdentity(taskData.inputDataHash(), group)
            .startAt(new java.util.Date(System.currentTimeMillis() + delay.toMillis()))
            .forJob(jobKey)
            .build();

    schedulerService.scheduleRetry(job, trigger);
  }
}
