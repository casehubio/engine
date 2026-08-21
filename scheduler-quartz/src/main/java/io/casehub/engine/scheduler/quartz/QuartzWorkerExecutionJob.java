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

import io.casehub.engine.common.internal.executor.WorkerExecutionOrchestrator;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@ApplicationScoped
public class QuartzWorkerExecutionJob implements Job {

  private static final Logger LOG = Logger.getLogger(QuartzWorkerExecutionJob.class);

  @Inject WorkerExecutionOrchestrator orchestrator;
  @Inject QuartzRetryService retryService;

  @Override
  public void execute(JobExecutionContext executionContext) {
    LOG.infof("Executing workflow task: %s", executionContext.getJobDetail().getKey());

    var jobData = executionContext.getMergedJobDataMap();
    WorkerTaskData taskData =
        new WorkerTaskData(
            Long.parseLong(jobData.getString("eventLogId")),
            jobData.getString("inputDataHash"),
            java.util.UUID.fromString(jobData.getString("caseHubInstanceUuid")),
            jobData.getString("workerId"),
            jobData.getString("tenancyId"),
            null,
            null);

    orchestrator.execute(
        taskData,
        (td, cause, msg) -> {
          WorkerRetryContext retryCtx =
              WorkerRetryContext.from(executionContext)
                  .withBindingName(td.bindingName())
                  .withSignalId(td.signalId());
          retryService.handleFailure(retryCtx, msg);
        });
  }
}
