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
import io.casehub.engine.common.internal.executor.RetryHandler;
import io.casehub.engine.common.internal.executor.RetryOrchestrator;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DbSchedulerRetryService implements RetryHandler {

  private static final Logger LOG = Logger.getLogger(DbSchedulerRetryService.class);

  @Inject RetryOrchestrator retryOrchestrator;
  @Inject DbSchedulerLifecycle lifecycle;

  @Override
  public void handleFailure(WorkerTaskData taskData, Throwable cause, String errorMessage) {
    retryOrchestrator.handleFailure(taskData, cause, errorMessage, this::reschedule);
  }

  private void reschedule(WorkerTaskData taskData, Duration delay) {
    Scheduler scheduler = lifecycle.getScheduler();
    OneTimeTask<ScheduledJobData> task =
        lifecycle.findTask(DbSchedulerLifecycle.TASK_WORKER_EXECUTION);

    String instanceId = taskData.caseId() + ":" + taskData.inputDataHash();
    ScheduledJobData jobData = ScheduledJobData.forWorkerExecution(taskData);
    Instant executionTime = Instant.now().plus(delay);

    scheduler.schedule(task.instance(instanceId, jobData), executionTime);
    LOG.infof(
        "Retry scheduled: worker=%s delay=%dms at %s",
        taskData.workerId(), delay.toMillis(), executionTime);
  }
}
