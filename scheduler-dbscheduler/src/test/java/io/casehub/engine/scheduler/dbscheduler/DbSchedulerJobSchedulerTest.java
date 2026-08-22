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

import static org.assertj.core.api.Assertions.assertThat;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import io.casehub.engine.common.internal.scheduler.JobIdentifier;
import io.casehub.engine.common.internal.scheduler.JobType;
import io.casehub.engine.common.internal.scheduler.ScheduleStrategy;
import io.casehub.engine.common.internal.scheduler.ScheduledJobRequest;
import io.casehub.engine.common.spi.scheduler.JobScheduler;
import io.casehub.engine.common.spi.scheduler.JobSchedulerContractTest;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class DbSchedulerJobSchedulerTest extends JobSchedulerContractTest {

  private static final String SCHEMA =
      """
      CREATE TABLE IF NOT EXISTS scheduled_tasks (
        task_name       VARCHAR(100) NOT NULL,
        task_instance   VARCHAR(500) NOT NULL,
        task_data       BLOB,
        execution_time  TIMESTAMP NOT NULL,
        picked          BOOLEAN NOT NULL DEFAULT FALSE,
        picked_by       VARCHAR(50),
        last_success    TIMESTAMP,
        last_failure    TIMESTAMP,
        consecutive_failures INT,
        last_heartbeat  TIMESTAMP,
        version         BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (task_name, task_instance)
      )
      """;

  private DataSource dataSource;
  private Scheduler dbScheduler;
  private final AtomicInteger executionCount = new AtomicInteger(0);

  private OneTimeTask<ScheduledJobData> scheduledTriggerTask;
  private OneTimeTask<ScheduledJobData> conditionalTriggerTask;
  private OneTimeTask<ScheduledJobData> milestoneSlaTask;
  private OneTimeTask<ScheduledJobData> signalTriggerTask;
  private OneTimeTask<ScheduledJobData> workerExecutionTask;

  @Override
  protected JobScheduler createJobScheduler() {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    dataSource = ds;

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(SCHEMA);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    scheduledTriggerTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_SCHEDULED_TRIGGER, ScheduledJobData.class)
            .execute((inst, ctx) -> executionCount.incrementAndGet());
    conditionalTriggerTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_CONDITIONAL_TRIGGER, ScheduledJobData.class)
            .execute((inst, ctx) -> executionCount.incrementAndGet());
    milestoneSlaTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_MILESTONE_SLA, ScheduledJobData.class)
            .execute((inst, ctx) -> executionCount.incrementAndGet());
    signalTriggerTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_SIGNAL_TRIGGER, ScheduledJobData.class)
            .execute((inst, ctx) -> executionCount.incrementAndGet());
    workerExecutionTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_WORKER_EXECUTION, ScheduledJobData.class)
            .execute((inst, ctx) -> executionCount.incrementAndGet());

    dbScheduler =
        Scheduler.create(
                dataSource,
                List.of(
                    scheduledTriggerTask,
                    conditionalTriggerTask,
                    milestoneSlaTask,
                    signalTriggerTask,
                    workerExecutionTask))
            .threads(1)
            .pollingInterval(Duration.ofSeconds(60))
            .build();

    DbSchedulerLifecycle lifecycle =
        new DbSchedulerLifecycle() {
          @Override
          Scheduler getScheduler() {
            return dbScheduler;
          }

          @Override
          DataSource getDataSource() {
            return dataSource;
          }

          @SuppressWarnings("unchecked")
          @Override
          <T> OneTimeTask<T> findTask(String taskName) {
            return switch (taskName) {
              case DbSchedulerLifecycle.TASK_SCHEDULED_TRIGGER ->
                  (OneTimeTask<T>) scheduledTriggerTask;
              case DbSchedulerLifecycle.TASK_CONDITIONAL_TRIGGER ->
                  (OneTimeTask<T>) conditionalTriggerTask;
              case DbSchedulerLifecycle.TASK_MILESTONE_SLA -> (OneTimeTask<T>) milestoneSlaTask;
              case DbSchedulerLifecycle.TASK_SIGNAL_TRIGGER -> (OneTimeTask<T>) signalTriggerTask;
              case DbSchedulerLifecycle.TASK_WORKER_EXECUTION ->
                  (OneTimeTask<T>) workerExecutionTask;
              default -> throw new IllegalArgumentException("Unknown task: " + taskName);
            };
          }
        };

    DbSchedulerJobScheduler jobScheduler = new DbSchedulerJobScheduler();
    jobScheduler.lifecycle = lifecycle;
    return jobScheduler;
  }

  @Override
  protected void destroyJobScheduler() {
    if (dbScheduler != null) {
      dbScheduler.stop();
    }
  }

  @Test
  void instanceId_format() {
    String id = DbSchedulerJobScheduler.toInstanceId(JobIdentifier.of("name", "group"));
    assertThat(id).isEqualTo("group:name");
  }

  @Test
  void schedule_cronExpression_storedInData() {
    JobIdentifier jobId = JobIdentifier.of("cron-test", "case-cron");
    scheduler.schedule(
        ScheduledJobRequest.builder()
            .jobId(jobId)
            .schedule(new ScheduleStrategy.CronSchedule("0 30 * * * ?"))
            .jobType(JobType.SIGNAL_TRIGGER)
            .data(Map.of("caseId", "cron-case", "bindingName", "sig1"))
            .build());

    assertThat(scheduler.exists(jobId)).isTrue();
  }
}
