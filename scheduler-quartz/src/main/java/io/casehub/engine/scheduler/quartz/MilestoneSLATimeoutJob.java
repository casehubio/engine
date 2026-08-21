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

import io.casehub.engine.common.internal.executor.MilestoneSLAData;
import io.casehub.engine.common.internal.executor.MilestoneSLAOrchestrator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@ApplicationScoped
public class MilestoneSLATimeoutJob implements Job {

  @Inject MilestoneSLAOrchestrator orchestrator;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      MilestoneSLAData data =
          new MilestoneSLAData(
              UUID.fromString(context.getMergedJobDataMap().getString("caseId")),
              context.getMergedJobDataMap().getString("milestoneName"));

      orchestrator.execute(data);
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }
}
