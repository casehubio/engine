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

import io.casehub.engine.common.internal.executor.MilestoneSLAData;
import io.casehub.engine.common.internal.executor.ScheduledSignalData;
import io.casehub.engine.common.internal.executor.ScheduledTriggerData;
import io.casehub.engine.common.internal.executor.WorkerTaskData;
import io.casehub.engine.common.internal.scheduler.JobType;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class ScheduledJobData implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private final JobType jobType;
  private final HashMap<String, String> data;
  private final String cronExpression;

  public ScheduledJobData(JobType jobType, Map<String, String> data, String cronExpression) {
    this.jobType = jobType;
    this.data = new HashMap<>(data);
    this.cronExpression = cronExpression;
  }

  public ScheduledJobData(JobType jobType, Map<String, String> data) {
    this(jobType, data, null);
  }

  public JobType jobType() {
    return jobType;
  }

  public Map<String, String> data() {
    return data;
  }

  public String cronExpression() {
    return cronExpression;
  }

  public WorkerTaskData toWorkerTaskData() {
    return WorkerTaskData.fromMap(data);
  }

  public ScheduledTriggerData toScheduledTriggerData() {
    return ScheduledTriggerData.fromMap(data);
  }

  public ScheduledSignalData toScheduledSignalData() {
    return ScheduledSignalData.fromMap(data);
  }

  public MilestoneSLAData toMilestoneSLAData() {
    return MilestoneSLAData.fromMap(data);
  }

  public static ScheduledJobData forWorkerExecution(WorkerTaskData taskData) {
    return new ScheduledJobData(JobType.WORKER_EXECUTION, taskData.toMap());
  }

  public static ScheduledJobData forScheduledTrigger(
      ScheduledTriggerData triggerData, String cronExpression) {
    return new ScheduledJobData(
        JobType.SCHEDULED_TRIGGER_UNCONDITIONAL, triggerData.toMap(), cronExpression);
  }

  public static ScheduledJobData forConditionalTrigger(
      ScheduledTriggerData triggerData, String cronExpression) {
    return new ScheduledJobData(
        JobType.SCHEDULED_TRIGGER_CONDITIONAL, triggerData.toMap(), cronExpression);
  }

  public static ScheduledJobData forMilestoneSLA(MilestoneSLAData slaData) {
    return new ScheduledJobData(JobType.MILESTONE_SLA_TIMEOUT, slaData.toMap());
  }

  public static ScheduledJobData forSignalTrigger(
      ScheduledSignalData signalData, String cronExpression) {
    return new ScheduledJobData(JobType.SIGNAL_TRIGGER, signalData.toMap(), cronExpression);
  }
}
