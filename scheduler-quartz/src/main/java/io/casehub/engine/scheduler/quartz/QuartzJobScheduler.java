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

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import io.casehub.engine.common.internal.scheduler.JobIdentifier;
import io.casehub.engine.common.internal.scheduler.JobType;
import io.casehub.engine.common.internal.scheduler.ScheduleStrategy;
import io.casehub.engine.common.internal.scheduler.ScheduleStrategy.CronSchedule;
import io.casehub.engine.common.internal.scheduler.ScheduleStrategy.DelaySchedule;
import io.casehub.engine.common.internal.scheduler.ScheduleStrategy.FixedAtSchedule;
import io.casehub.engine.common.internal.scheduler.ScheduledJobRequest;
import io.casehub.engine.common.spi.scheduler.JobScheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;

/**
 * Quartz-based implementation of {@link JobScheduler}.
 *
 * <p>Converts domain scheduling models to Quartz-specific types and delegates to the Quartz {@link
 * Scheduler}.
 */
@ApplicationScoped
public class QuartzJobScheduler implements JobScheduler {

  private static final Logger LOG = Logger.getLogger(QuartzJobScheduler.class);

  @Inject Scheduler quartz;

  @Override
  public void schedule(ScheduledJobRequest request) {
    Class<? extends Job> jobClass = resolveQuartzJobClass(request.getJobType());

    JobKey jobKey = toQuartzJobKey(request.getJobId());
    JobDetail job = toQuartzJob(jobClass, request, jobKey);
    Trigger trigger = toQuartzTrigger(request, jobKey);

    try {
      scheduleOrReplaceJob(job, trigger);
      LOG.debugf("Scheduled job: %s with schedule: %s", jobKey, request.getSchedule());
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to schedule job: " + request.getJobId(), e);
    }
  }

  private void scheduleOrReplaceJob(JobDetail job, Trigger trigger) throws SchedulerException {
    try {
      quartz.scheduleJob(job, trigger);
      return;
    } catch (ObjectAlreadyExistsException ignored) {
      LOG.debugf("Job already exists, replacing schedule: %s", job.getKey());
    }

    quartz.addJob(job, true, true);
    if (quartz.rescheduleJob(trigger.getKey(), trigger) != null) {
      return;
    }

    try {
      quartz.scheduleJob(trigger);
    } catch (ObjectAlreadyExistsException ignored) {
      quartz.rescheduleJob(trigger.getKey(), trigger);
    }
  }

  @Override
  public void schedule(ScheduledJobRequest.Builder builder) {
    schedule(builder.build());
  }

  private Class<? extends Job> resolveQuartzJobClass(JobType jobType) {
    if (jobType == null) {
      throw new IllegalArgumentException("JobType must not be null on ScheduledJobRequest");
    }
    return switch (jobType) {
      case SCHEDULED_TRIGGER_UNCONDITIONAL -> ScheduledTriggerJob.class;
      case SCHEDULED_TRIGGER_CONDITIONAL -> ConditionalScheduledTriggerJob.class;
      case MILESTONE_SLA_TIMEOUT -> MilestoneSLATimeoutJob.class;
      case SIGNAL_TRIGGER -> ScheduledSignalJob.class;
      case WORKER_EXECUTION ->
          throw new IllegalArgumentException(
              "WORKER_EXECUTION jobs are not scheduled via JobScheduler — use WorkerExecutionManager.submit()");
    };
  }

  @Override
  public boolean cancel(JobIdentifier jobId) {
    try {
      JobKey jobKey = toQuartzJobKey(jobId);
      boolean deleted = quartz.deleteJob(jobKey);

      if (deleted) {
        LOG.debugf("Cancelled job: %s", jobId);
      } else {
        LOG.debugf("Job not found for cancellation: %s", jobId);
      }

      return deleted;
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to cancel job: " + jobId, e);
    }
  }

  @Override
  public int cancelGroup(String groupName) {
    try {
      GroupMatcher<JobKey> matcher = GroupMatcher.jobGroupEquals(groupName);
      Set<JobKey> jobKeys = quartz.getJobKeys(matcher);

      if (!jobKeys.isEmpty()) {
        quartz.deleteJobs(new ArrayList<>(jobKeys));
        LOG.debugf("Cancelled %d jobs in group: %s", jobKeys.size(), groupName);
      } else {
        LOG.debugf("No jobs found in group: %s", groupName);
      }

      return jobKeys.size();
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to cancel group: " + groupName, e);
    }
  }

  @Override
  public boolean exists(JobIdentifier jobId) {
    try {
      JobKey jobKey = toQuartzJobKey(jobId);
      return quartz.checkExists(jobKey);
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to check job existence: " + jobId, e);
    }
  }

  private JobKey toQuartzJobKey(JobIdentifier jobId) {
    return new JobKey(jobId.getName(), jobId.getGroup());
  }

  private JobDetail toQuartzJob(
      Class<? extends Job> jobClass, ScheduledJobRequest request, JobKey jobKey) {
    JobDataMap jobDataMap = new JobDataMap();
    for (Map.Entry<String, Object> entry : request.getData().entrySet()) {
      jobDataMap.put(entry.getKey(), entry.getValue());
    }

    return newJob(jobClass)
        .withIdentity(jobKey)
        .storeDurably(false)
        .usingJobData(jobDataMap)
        .build();
  }

  private Trigger toQuartzTrigger(ScheduledJobRequest request, JobKey jobKey) {
    ScheduleStrategy schedule = request.getSchedule();

    if (schedule instanceof CronSchedule cron) {
      return createCronTrigger(jobKey, cron.expression());
    } else if (schedule instanceof DelaySchedule delay) {
      return createDelayTrigger(jobKey, delay.delayMillis());
    } else if (schedule instanceof FixedAtSchedule fixed) {
      return createFixedAtTrigger(jobKey, fixed.executeAtMillis());
    } else {
      throw new IllegalArgumentException("Unknown schedule type: " + schedule.getClass());
    }
  }

  private Trigger createCronTrigger(JobKey jobKey, String cronExpression) {
    return newTrigger()
        .withIdentity("trigger-" + jobKey.getName(), jobKey.getGroup())
        .forJob(jobKey)
        .withSchedule(cronSchedule(cronExpression))
        .build();
  }

  private Trigger createDelayTrigger(JobKey jobKey, long delayMillis) {
    Date startTime = new Date(System.currentTimeMillis() + delayMillis);

    return newTrigger()
        .withIdentity("trigger-" + jobKey.getName(), jobKey.getGroup())
        .forJob(jobKey)
        .startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(0)) // one-shot
        .build();
  }

  private Trigger createFixedAtTrigger(JobKey jobKey, long executeAtMillis) {
    Date startTime = new Date(executeAtMillis);

    return newTrigger()
        .withIdentity("trigger-" + jobKey.getName(), jobKey.getGroup())
        .forJob(jobKey)
        .startAt(startTime)
        .withSchedule(simpleSchedule().withRepeatCount(0)) // one-shot
        .build();
  }
}
