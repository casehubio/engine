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
package io.casehub.engine.internal.worker;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.JobDetail;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

@ApplicationScoped
public class WorkerExecutionScheduler {

  @Inject Scheduler quartz;

  public void scheduleOrReschedule(JobDetail job, Trigger trigger) {
    try {
      quartz.scheduleJob(job, trigger);
    } catch (ObjectAlreadyExistsException e) {
      // Job is already queued in Quartz — it will execute once. Rescheduling would fire it
      // a second time, which is wrong. The idempotency key guarantees one execution per
      // distinct (worker, capability, inputDataHash) combination.
    } catch (SchedulerException e) {
      throw new RuntimeException("Quartz scheduling failed for jobKey=" + job.getKey(), e);
    }
  }

  public Uni<Void> scheduleOrRescheduleAsync(JobDetail job, Trigger trigger) {
    return Uni.createFrom()
        .item(
            () -> {
              scheduleOrReschedule(job, trigger);
              return (Void) null;
            })
        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        .replaceWithVoid();
  }
}
