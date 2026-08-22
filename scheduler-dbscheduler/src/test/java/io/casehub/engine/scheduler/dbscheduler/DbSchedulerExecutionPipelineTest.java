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
import io.casehub.engine.common.internal.executor.MilestoneSLAData;
import io.casehub.engine.common.internal.executor.ScheduledSignalData;
import io.casehub.engine.common.internal.executor.ScheduledTriggerData;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbSchedulerExecutionPipelineTest {

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
  private Scheduler scheduler;

  private final CopyOnWriteArrayList<WorkerTaskData> executedWorkerTasks =
      new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<ScheduledTriggerData> executedTriggers =
      new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<MilestoneSLAData> executedMilestones =
      new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<ScheduledSignalData> executedSignals =
      new CopyOnWriteArrayList<>();

  private CountDownLatch latch;

  private OneTimeTask<ScheduledJobData> workerExecutionTask;
  private OneTimeTask<ScheduledJobData> scheduledTriggerTask;
  private OneTimeTask<ScheduledJobData> conditionalTriggerTask;
  private OneTimeTask<ScheduledJobData> milestoneSlaTask;
  private OneTimeTask<ScheduledJobData> signalTriggerTask;

  @BeforeEach
  void setUp() throws SQLException {
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL("jdbc:h2:mem:pipeline-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    ds.setUser("sa");
    ds.setPassword("");
    dataSource = ds;

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(SCHEMA);
    }

    workerExecutionTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_WORKER_EXECUTION, ScheduledJobData.class)
            .execute(
                (inst, ctx) -> {
                  executedWorkerTasks.add(inst.getData().toWorkerTaskData());
                  latch.countDown();
                });

    scheduledTriggerTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_SCHEDULED_TRIGGER, ScheduledJobData.class)
            .execute(
                (inst, ctx) -> {
                  executedTriggers.add(inst.getData().toScheduledTriggerData());
                  latch.countDown();
                });

    conditionalTriggerTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_CONDITIONAL_TRIGGER, ScheduledJobData.class)
            .execute(
                (inst, ctx) -> {
                  executedTriggers.add(inst.getData().toScheduledTriggerData());
                  latch.countDown();
                });

    milestoneSlaTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_MILESTONE_SLA, ScheduledJobData.class)
            .execute(
                (inst, ctx) -> {
                  executedMilestones.add(inst.getData().toMilestoneSLAData());
                  latch.countDown();
                });

    signalTriggerTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_SIGNAL_TRIGGER, ScheduledJobData.class)
            .execute(
                (inst, ctx) -> {
                  executedSignals.add(inst.getData().toScheduledSignalData());
                  latch.countDown();
                });

    scheduler =
        Scheduler.create(
                dataSource,
                List.of(
                    workerExecutionTask,
                    scheduledTriggerTask,
                    conditionalTriggerTask,
                    milestoneSlaTask,
                    signalTriggerTask))
            .threads(2)
            .pollingInterval(Duration.ofMillis(100))
            .enableImmediateExecution()
            .build();

    scheduler.start();
  }

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.stop();
    }
  }

  @Test
  void workerExecution_scheduledAndPickedUp() throws InterruptedException {
    latch = new CountDownLatch(1);
    UUID caseId = UUID.randomUUID();
    WorkerTaskData taskData =
        new WorkerTaskData(1L, "hash1", caseId, "worker1", "tenant1", "bind1", null);
    ScheduledJobData jobData = ScheduledJobData.forWorkerExecution(taskData);

    scheduler.schedule(workerExecutionTask.instance(caseId + ":hash1", jobData), Instant.now());

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executedWorkerTasks).hasSize(1);
    assertThat(executedWorkerTasks.get(0).caseId()).isEqualTo(caseId);
    assertThat(executedWorkerTasks.get(0).workerId()).isEqualTo("worker1");
    assertThat(executedWorkerTasks.get(0).bindingName()).isEqualTo("bind1");
  }

  @Test
  void scheduledTrigger_scheduledAndPickedUp() throws InterruptedException {
    latch = new CountDownLatch(1);
    UUID caseId = UUID.randomUUID();
    ScheduledTriggerData triggerData = new ScheduledTriggerData(caseId, "bind1", "cap1", "worker1");
    ScheduledJobData jobData = ScheduledJobData.forScheduledTrigger(triggerData, null);

    scheduler.schedule(scheduledTriggerTask.instance(caseId + ":trigger", jobData), Instant.now());

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executedTriggers).hasSize(1);
    assertThat(executedTriggers.get(0).caseId()).isEqualTo(caseId);
    assertThat(executedTriggers.get(0).capabilityName()).isEqualTo("cap1");
  }

  @Test
  void milestoneSLA_scheduledAndPickedUp() throws InterruptedException {
    latch = new CountDownLatch(1);
    UUID caseId = UUID.randomUUID();
    MilestoneSLAData slaData = new MilestoneSLAData(caseId, "milestone1");
    ScheduledJobData jobData = ScheduledJobData.forMilestoneSLA(slaData);

    scheduler.schedule(milestoneSlaTask.instance(caseId + ":milestone1", jobData), Instant.now());

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executedMilestones).hasSize(1);
    assertThat(executedMilestones.get(0).milestoneName()).isEqualTo("milestone1");
  }

  @Test
  void signalTrigger_scheduledAndPickedUp() throws InterruptedException {
    latch = new CountDownLatch(1);
    UUID caseId = UUID.randomUUID();
    ScheduledSignalData signalData =
        new ScheduledSignalData(caseId, "bind1", "{\"key\":\"val\"}", false);
    ScheduledJobData jobData = ScheduledJobData.forSignalTrigger(signalData, null);

    scheduler.schedule(signalTriggerTask.instance(caseId + ":signal", jobData), Instant.now());

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executedSignals).hasSize(1);
    assertThat(executedSignals.get(0).signalPayload()).isEqualTo("{\"key\":\"val\"}");
  }

  @Test
  void multipleTasks_executedConcurrently() throws InterruptedException {
    latch = new CountDownLatch(3);
    UUID case1 = UUID.randomUUID();
    UUID case2 = UUID.randomUUID();
    UUID case3 = UUID.randomUUID();

    scheduler.schedule(
        workerExecutionTask.instance(
            case1 + ":h1",
            ScheduledJobData.forWorkerExecution(
                new WorkerTaskData(1L, "h1", case1, "w1", "t1", null, null))),
        Instant.now());
    scheduler.schedule(
        workerExecutionTask.instance(
            case2 + ":h2",
            ScheduledJobData.forWorkerExecution(
                new WorkerTaskData(2L, "h2", case2, "w2", "t1", null, null))),
        Instant.now());
    scheduler.schedule(
        milestoneSlaTask.instance(
            case3 + ":m1", ScheduledJobData.forMilestoneSLA(new MilestoneSLAData(case3, "m1"))),
        Instant.now());

    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(executedWorkerTasks).hasSize(2);
    assertThat(executedMilestones).hasSize(1);
  }

  @Test
  void cronTrigger_reschedulesAfterExecution() throws Exception {
    JdbcDataSource cronDs = new JdbcDataSource();
    cronDs.setURL("jdbc:h2:mem:cron-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
    cronDs.setUser("sa");
    cronDs.setPassword("");
    try (Connection conn = cronDs.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(SCHEMA);
    }

    java.util.concurrent.atomic.AtomicInteger fireCount =
        new java.util.concurrent.atomic.AtomicInteger(0);
    CountDownLatch cronLatch = new CountDownLatch(2);
    java.util.concurrent.atomic.AtomicReference<com.github.kagkarlsson.scheduler.Scheduler>
        schedulerRef = new java.util.concurrent.atomic.AtomicReference<>();

    OneTimeTask<ScheduledJobData> cronTask =
        Tasks.oneTime(DbSchedulerLifecycle.TASK_SCHEDULED_TRIGGER, ScheduledJobData.class)
            .execute(
                (inst, ctx) -> {
                  int count = fireCount.incrementAndGet();
                  cronLatch.countDown();
                  String cron = inst.getData().cronExpression();
                  if (cron != null && count < 3) {
                    String baseId =
                        inst.getId().contains("#")
                            ? inst.getId().substring(0, inst.getId().lastIndexOf('#'))
                            : inst.getId();
                    String newId = baseId + "#" + count;
                    schedulerRef
                        .get()
                        .schedule(
                            cronTask(inst.getTaskName(), newId, inst.getData()),
                            java.time.Instant.now().plusMillis(50));
                  }
                });

    com.github.kagkarlsson.scheduler.Scheduler cronScheduler =
        com.github.kagkarlsson.scheduler.Scheduler.create(cronDs, List.of(cronTask))
            .threads(1)
            .pollingInterval(java.time.Duration.ofMillis(50))
            .enableImmediateExecution()
            .build();
    schedulerRef.set(cronScheduler);
    cronScheduler.start();

    try {
      UUID caseId = UUID.randomUUID();
      ScheduledTriggerData triggerData =
          new ScheduledTriggerData(caseId, "bind1", "cap1", "worker1");
      ScheduledJobData jobData = ScheduledJobData.forScheduledTrigger(triggerData, "0 * * * * ?");

      cronScheduler.schedule(
          cronTask.instance(caseId + ":cron-trigger", jobData), java.time.Instant.now());

      assertThat(cronLatch.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(fireCount.get()).isGreaterThanOrEqualTo(2);
    } finally {
      cronScheduler.stop();
    }
  }

  private static com.github.kagkarlsson.scheduler.task.TaskInstance<ScheduledJobData> cronTask(
      String taskName, String instanceId, ScheduledJobData data) {
    return new com.github.kagkarlsson.scheduler.task.TaskInstance<>(taskName, instanceId, data);
  }
}
