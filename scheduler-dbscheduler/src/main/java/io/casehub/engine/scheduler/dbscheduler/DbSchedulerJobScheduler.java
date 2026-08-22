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
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import io.casehub.engine.common.internal.scheduler.JobIdentifier;
import io.casehub.engine.common.internal.scheduler.ScheduleStrategy;
import io.casehub.engine.common.internal.scheduler.ScheduledJobRequest;
import io.casehub.engine.common.spi.scheduler.JobScheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DbSchedulerJobScheduler implements JobScheduler {

  private static final Logger LOG = Logger.getLogger(DbSchedulerJobScheduler.class);

  @Inject DbSchedulerLifecycle lifecycle;

  @Override
  public void schedule(ScheduledJobRequest request) {
    Scheduler scheduler = lifecycle.getScheduler();
    String taskName = lifecycle.taskNameForJobType(request.getJobType());
    String instanceId = toInstanceId(request.getJobId());

    Map<String, String> stringData = toStringMap(request.getData());
    String cronExpression = extractCronExpression(request.getSchedule());
    ScheduledJobData jobData =
        new ScheduledJobData(request.getJobType(), stringData, cronExpression);

    OneTimeTask<ScheduledJobData> task = lifecycle.findTask(taskName);
    Instant executionTime = computeExecutionTime(request.getSchedule());

    scheduler.schedule(task.instance(instanceId, jobData), executionTime);
    LOG.debugf("Scheduled %s:%s at %s", taskName, instanceId, executionTime);
  }

  @Override
  public void schedule(ScheduledJobRequest.Builder builder) {
    schedule(builder.build());
  }

  @Override
  public boolean cancel(JobIdentifier jobId) {
    Scheduler scheduler = lifecycle.getScheduler();
    String instanceId = toInstanceId(jobId);

    for (String taskName :
        new String[] {
          DbSchedulerLifecycle.TASK_SCHEDULED_TRIGGER,
          DbSchedulerLifecycle.TASK_CONDITIONAL_TRIGGER,
          DbSchedulerLifecycle.TASK_MILESTONE_SLA,
          DbSchedulerLifecycle.TASK_SIGNAL_TRIGGER,
          DbSchedulerLifecycle.TASK_WORKER_EXECUTION
        }) {
      try {
        scheduler.cancel(TaskInstanceId.of(taskName, instanceId));
        LOG.debugf("Cancelled %s:%s", taskName, instanceId);
        return true;
      } catch (Exception ignored) {
        // task not found for this type, try next
      }
    }
    LOG.debugf("No task found to cancel: %s", instanceId);
    return false;
  }

  @Override
  public int cancelGroup(String groupName) {
    DataSource ds = lifecycle.getDataSource();
    Scheduler scheduler = lifecycle.getScheduler();
    String prefix = groupName + ":";
    int count = 0;

    try (Connection conn = ds.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "SELECT task_name, task_instance FROM scheduled_tasks WHERE task_instance LIKE ?")) {
      ps.setString(1, prefix + "%");
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String taskName = rs.getString("task_name");
          String taskInstance = rs.getString("task_instance");
          try {
            scheduler.cancel(TaskInstanceId.of(taskName, taskInstance));
            count++;
          } catch (Exception ignored) {
            // already picked or completed
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to cancel group: " + groupName, e);
    }

    if (count > 0) {
      LOG.debugf("Cancelled %d tasks in group: %s", count, groupName);
    }
    return count;
  }

  @Override
  public boolean exists(JobIdentifier jobId) {
    DataSource ds = lifecycle.getDataSource();
    String instanceId = toInstanceId(jobId);

    try (Connection conn = ds.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "SELECT 1 FROM scheduled_tasks WHERE task_instance = ? LIMIT 1")) {
      ps.setString(1, instanceId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check existence: " + jobId, e);
    }
  }

  static String toInstanceId(JobIdentifier jobId) {
    return jobId.getGroup() + ":" + jobId.getName();
  }

  private static Instant computeExecutionTime(ScheduleStrategy schedule) {
    return switch (schedule) {
      case ScheduleStrategy.DelaySchedule delay -> Instant.now().plusMillis(delay.delayMillis());
      case ScheduleStrategy.FixedAtSchedule fixed -> Instant.ofEpochMilli(fixed.executeAtMillis());
      case ScheduleStrategy.CronSchedule cron -> {
        Optional<Instant> next = CronUtils.nextExecution(cron.expression());
        yield next.orElseGet(Instant::now);
      }
    };
  }

  private static String extractCronExpression(ScheduleStrategy schedule) {
    if (schedule instanceof ScheduleStrategy.CronSchedule cron) {
      return cron.expression();
    }
    return null;
  }

  private static Map<String, String> toStringMap(Map<String, Object> data) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      result.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
    }
    return result;
  }
}
