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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.ExecutionPolicy;
import io.casehub.api.model.RetryPolicy;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.recovery.WorkerExecutionRecoveryService;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerRetriesExhaustedEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.ReactiveUtils;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;
import org.jboss.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Trigger;

@ApplicationScoped
public class WorkerExecutionJobListener implements JobListener {

  @Inject Vertx vertx;

  @Inject WorkerExecutionScheduler workerExecutionScheduler;

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject EventBus eventBus;

  @Inject WorkerExecutionRecoveryService workerExecutionRecoveryService;

  private static final Logger LOG = Logger.getLogger(WorkerExecutionJobListener.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public String getName() {
    return WorkerExecutionJobListener.class.getSimpleName();
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    if (isWorkflowExecutionJob(context)) {
      return;
    }

    String jobName = context.getJobDetail().getKey().toString();
    String idempotency = context.getMergedJobDataMap().getString("inputDataHash");
    LOG.infof("Job is about to be executed: %s, idempotency=%s", jobName, idempotency);

    EventLog eventLog =
        createEventLog(
            context,
            CaseHubEventType.WORKER_EXECUTION_STARTED,
            OBJECT_MAPPER.createObjectNode().put("inputDataHash", idempotency));

    persistEventLog(jobName, eventLog)
        .subscribe()
        .with(
            ignored -> LOG.debugf("Persisted start event for %s", jobName),
            ex -> LOG.errorf(ex, "Failed to persist start event for %s", jobName));
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {
    if (isWorkflowExecutionJob(context)) {
      return;
    }

    String jobName = context.getJobDetail().getKey().toString();
    LOG.info("Job execution was vetoed for job: " + jobName);
  }

  /** We log success at WorkflowExecutionCompletedHandler.onWorkflowExecutionCompletedHandler */
  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    if (isWorkflowExecutionJob(context)) {
      return;
    }

    if (jobException != null) {
      String jobName = context.getJobDetail().getKey().toString();
      String idempotency = context.getMergedJobDataMap().getString("inputDataHash");
      LOG.errorf("Job failed: %s, Error: %s", jobName, jobException.getMessage());

      EventLog eventLog =
          createEventLog(
              context,
              CaseHubEventType.WORKER_EXECUTION_FAILED,
              OBJECT_MAPPER
                  .createObjectNode()
                  .put("inputDataHash", idempotency)
                  .put("errorMessage", jobException.getMessage()));

      persistEventLog(jobName, eventLog)
          .subscribe()
          .with(
              success -> maybeRescheduleJob(context),
              ex -> LOG.errorf(ex, "Failed to persist and reschedule job: %s", jobName));
    }
  }

  private boolean isWorkflowExecutionJob(JobExecutionContext context) {
    return !WorkerExecutionTask.class.equals(context.getJobDetail().getJobClass());
  }

  private void maybeRescheduleJob(JobExecutionContext context) {
    String jobName = context.getJobDetail().getKey().toString();
    String caseHubInstanceUuid = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
    String workerId = context.getMergedJobDataMap().getString("workerId");
    String idempotency = context.getMergedJobDataMap().getString("inputDataHash");
    UUID caseId = UUID.fromString(caseHubInstanceUuid);

    workerExecutionRecoveryService
        .loadOrRestoreCaseInstance(caseId)
        .map(instance -> resolveRetryPolicy(jobName, instance, workerId))
        .subscribe()
        .with(
            retryPolicy -> {

              // TODO use default policy
              if (retryPolicy == null) {
                return;
              }
              countFailedAttempts(caseId, workerId, idempotency)
                  .subscribe()
                  .with(
                      failureCount -> {
                        if (failureCount < retryPolicy.maxAttempts()) {
                          LOG.infof(
                              "Rescheduling worker %s: attempt %d/%d, delayMs=%d",
                              workerId,
                              failureCount + 1,
                              retryPolicy.maxAttempts(),
                              retryPolicy.delayMs());
                          rescheduleJob(context, retryPolicy);
                        } else {
                          LOG.warnf(
                              "Worker %s exhausted all %d retry attempts for case %s",
                              workerId, retryPolicy.maxAttempts(), caseHubInstanceUuid);
                          eventBus.publish(
                              EventBusAddresses.WORKER_RETRIES_EXHAUSTED,
                              new WorkerRetriesExhaustedEvent(caseId, workerId, idempotency));
                        }
                      },
                      ex ->
                          LOG.errorf(
                              ex, "Failed to count failed attempts for worker %s", workerId));
            },
            ex ->
                LOG.errorf(
                    ex,
                    "Failed to reschedule worker %s for case %s",
                    workerId,
                    caseHubInstanceUuid));
  }

  private RetryPolicy resolveRetryPolicy(String jobName, CaseInstance instance, String workerId) {
    CaseHubDefinition definition =
        caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());
    if (definition == null) {
      LOG.errorf("Cannot reschedule job %s: CaseHubDefinition not found", jobName);
      throw new RuntimeException("CaseHubDefinition not found for caseId=" + instance.getUuid());
    }

    Worker worker =
        definition.getWorkers().stream()
            .filter(w -> w.getName().equals(workerId))
            .findFirst()
            .orElse(null);

    if (worker == null) {
      LOG.errorf("Cannot reschedule job %s: Worker not found: %s", jobName, workerId);
      throw new RuntimeException("Worker not found in case definition: " + workerId);
    }

    ExecutionPolicy executionPolicy = worker.getExecutionPolicy();
    if (executionPolicy == null || executionPolicy.retries() == null) {
      return new ExecutionPolicy().retries();
    }
    return executionPolicy.retries();
  }

  private void rescheduleJob(JobExecutionContext context, RetryPolicy retryPolicy) {
    String idempotency = context.getMergedJobDataMap().getString("inputDataHash");
    String group = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
    JobKey jobKey = new JobKey(idempotency, group);

    JobDetail job =
        newJob(WorkerExecutionTask.class)
            .withIdentity(jobKey)
            .storeDurably(false)
            .usingJobData(context.getMergedJobDataMap())
            .build();

    int delayMs = retryPolicy.delayMs() > 0 ? retryPolicy.delayMs() : 0;

    Trigger trigger =
        newTrigger()
            .withIdentity(idempotency, group)
            .startAt(new Date(System.currentTimeMillis() + delayMs))
            .forJob(jobKey)
            .build();

    workerExecutionScheduler
        .scheduleOrRescheduleAsync(job, trigger)
        .subscribe()
        .with(
            ignored -> LOG.infof("Rescheduled job: %s", jobKey),
            ex -> LOG.errorf(ex, "Failed to reschedule job: %s", jobKey));
  }

  private static EventLog createEventLog(
      JobExecutionContext context, CaseHubEventType eventType, JsonNode metadata) {
    String caseHubInstanceUuid = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
    String workerId = context.getMergedJobDataMap().getString("workerId");

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(UUID.fromString(caseHubInstanceUuid));
    eventLog.setWorkerId(workerId);
    eventLog.setEventType(eventType);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(context.getFireTime().toInstant());
    eventLog.setMetadata(metadata);
    return eventLog;
  }

  private Uni<Void> persistEventLog(String jobName, EventLog eventLog) {
    return runOnSafeVertxContext(
            () -> Panache.withTransaction(() -> PanacheEntityBase.persist(eventLog)))
        .onFailure()
        .invoke(ex -> LOG.errorf(ex, "Failed to persist and reschedule job: %s", jobName))
        .replaceWithVoid();
  }

  // TODO metadata->>'idempotency' way faster but not very stable
  private Uni<Long> countFailedAttempts(UUID caseId, String workerId, String idempotency) {
    return Panache.withSession(
        () ->
            EventLog.<EventLog>find(
                    "caseId = ?1 and workerId = ?2 and eventType = ?3",
                    caseId,
                    workerId,
                    CaseHubEventType.WORKER_EXECUTION_FAILED)
                .list()
                .map(
                    eventLogs ->
                        eventLogs.stream()
                            .filter(
                                eventLog -> {
                                  JsonNode metadata = eventLog.getMetadata();
                                  JsonNode idempotencyNode =
                                      metadata == null ? null : metadata.get("inputDataHash");
                                  return idempotencyNode != null
                                      && idempotency.equals(idempotencyNode.asText());
                                })
                            .count()));
  }

  private <T> Uni<T> runOnSafeVertxContext(Supplier<Uni<? extends T>> action) {
    return ReactiveUtils.runOnSafeVertxContext(vertx, action);
  }
}
