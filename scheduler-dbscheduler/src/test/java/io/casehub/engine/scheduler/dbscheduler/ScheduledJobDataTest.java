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

import io.casehub.engine.common.internal.executor.MilestoneSLAData;
import io.casehub.engine.common.internal.executor.ScheduledSignalData;
import io.casehub.engine.common.internal.executor.ScheduledTriggerData;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import io.casehub.engine.common.internal.scheduler.JobType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduledJobDataTest {

  @Test
  void workerExecution_roundTrip() {
    UUID caseId = UUID.randomUUID();
    WorkerTaskData original =
        new WorkerTaskData(42L, "hash123", caseId, "worker1", "tenant1", "binding1", null);

    ScheduledJobData jobData = ScheduledJobData.forWorkerExecution(original);

    assertThat(jobData.jobType()).isEqualTo(JobType.WORKER_EXECUTION);
    assertThat(jobData.cronExpression()).isNull();

    WorkerTaskData restored = jobData.toWorkerTaskData();
    assertThat(restored.eventLogId()).isEqualTo(42L);
    assertThat(restored.inputDataHash()).isEqualTo("hash123");
    assertThat(restored.caseId()).isEqualTo(caseId);
    assertThat(restored.workerId()).isEqualTo("worker1");
    assertThat(restored.tenancyId()).isEqualTo("tenant1");
    assertThat(restored.bindingName()).isEqualTo("binding1");
  }

  @Test
  void scheduledTrigger_roundTrip() {
    UUID caseId = UUID.randomUUID();
    ScheduledTriggerData original = new ScheduledTriggerData(caseId, "bind1", "cap1", "worker1");

    ScheduledJobData jobData = ScheduledJobData.forScheduledTrigger(original, "0 0 * * * ?");

    assertThat(jobData.jobType()).isEqualTo(JobType.SCHEDULED_TRIGGER_UNCONDITIONAL);
    assertThat(jobData.cronExpression()).isEqualTo("0 0 * * * ?");

    ScheduledTriggerData restored = jobData.toScheduledTriggerData();
    assertThat(restored.caseId()).isEqualTo(caseId);
    assertThat(restored.bindingName()).isEqualTo("bind1");
    assertThat(restored.capabilityName()).isEqualTo("cap1");
    assertThat(restored.workerName()).isEqualTo("worker1");
  }

  @Test
  void milestoneSLA_roundTrip() {
    UUID caseId = UUID.randomUUID();
    MilestoneSLAData original = new MilestoneSLAData(caseId, "milestone1");

    ScheduledJobData jobData = ScheduledJobData.forMilestoneSLA(original);

    assertThat(jobData.jobType()).isEqualTo(JobType.MILESTONE_SLA_TIMEOUT);
    assertThat(jobData.cronExpression()).isNull();

    MilestoneSLAData restored = jobData.toMilestoneSLAData();
    assertThat(restored.caseId()).isEqualTo(caseId);
    assertThat(restored.milestoneName()).isEqualTo("milestone1");
  }

  @Test
  void signalTrigger_roundTrip() {
    UUID caseId = UUID.randomUUID();
    ScheduledSignalData original =
        new ScheduledSignalData(caseId, "bind1", "{\"key\":\"val\"}", true);

    ScheduledJobData jobData = ScheduledJobData.forSignalTrigger(original, "0 30 * * * ?");

    assertThat(jobData.jobType()).isEqualTo(JobType.SIGNAL_TRIGGER);
    assertThat(jobData.cronExpression()).isEqualTo("0 30 * * * ?");

    ScheduledSignalData restored = jobData.toScheduledSignalData();
    assertThat(restored.caseId()).isEqualTo(caseId);
    assertThat(restored.bindingName()).isEqualTo("bind1");
    assertThat(restored.signalPayload()).isEqualTo("{\"key\":\"val\"}");
    assertThat(restored.hasCondition()).isTrue();
  }

  @Test
  void javaSerialization_roundTrip() throws Exception {
    UUID caseId = UUID.randomUUID();
    WorkerTaskData original = new WorkerTaskData(99L, "hashXYZ", caseId, "w1", "t1", null, null);
    ScheduledJobData jobData = ScheduledJobData.forWorkerExecution(original);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(jobData);
    }

    ScheduledJobData deserialized;
    try (ObjectInputStream ois =
        new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      deserialized = (ScheduledJobData) ois.readObject();
    }

    assertThat(deserialized.jobType()).isEqualTo(JobType.WORKER_EXECUTION);
    WorkerTaskData restored = deserialized.toWorkerTaskData();
    assertThat(restored.eventLogId()).isEqualTo(99L);
    assertThat(restored.caseId()).isEqualTo(caseId);
  }
}
