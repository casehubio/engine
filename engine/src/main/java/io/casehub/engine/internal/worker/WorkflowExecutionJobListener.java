package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.ExecutionPolicy;
import io.casehub.api.model.RetryPolicy;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerRetriesExhaustedEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Trigger;

import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@ApplicationScoped
public class WorkflowExecutionJobListener implements JobListener {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Inject
    Vertx vertx;

    @Inject
    WorkflowExecutionScheduler workflowExecutionScheduler;

    @Inject
    CaseDefinitionRegistry caseDefinitionRegistry;

    @Inject
    CaseInstanceCache caseInstanceCache;

    @Inject
    EventBus eventBus;

    private static final Logger LOG = Logger.getLogger(WorkflowExecutionJobListener.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String getName() {
        return WorkflowExecutionJobListener.class.getSimpleName();
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        if (isWorkflowExecutionJob(context)) {
            return;
        }

        String jobName = context.getJobDetail().getKey().toString();
        String idempotency = context.getMergedJobDataMap().getString("idempotency");
        LOG.infof("Job is about to be executed: %s, idempotency=%s", jobName, idempotency);

        EventLog eventLog = createEventLog(context, CaseHubEventType.WORKER_EXECUTION_STARTED,
                OBJECT_MAPPER.createObjectNode().put("idempotency", idempotency));

        persistEventLog(jobName, eventLog).subscribe().with(
                ignored -> LOG.debugf("Persisted start event for %s", jobName),
                ex -> LOG.errorf(ex, "Failed to persist start event for %s", jobName)
        );
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        if (isWorkflowExecutionJob(context)) {
            return;
        }

        String jobName = context.getJobDetail().getKey().toString();
        LOG.info("Job execution was vetoed for job: " + jobName);
    }

    /**
     * We log success at WorkflowExecutionCompletedHandler.onWorkflowExecutionCompletedHandler
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (isWorkflowExecutionJob(context)) {
            return;
        }

        if (jobException != null) {
            String jobName = context.getJobDetail().getKey().toString();
            String idempotency = context.getMergedJobDataMap().getString("idempotency");
            LOG.errorf("Job failed: %s, Error: %s", jobName, jobException.getMessage());

            EventLog eventLog = createEventLog(context, CaseHubEventType.WORKER_EXECUTION_FAILED,
                    OBJECT_MAPPER.createObjectNode()
                            .put("idempotency", idempotency)
                            .put("errorMessage", jobException.getMessage()));

            persistEventLog(jobName, eventLog).subscribe().with(
                    success -> maybeRescheduleJob(context),
                    ex -> LOG.errorf(ex, "Failed to persist and reschedule job: %s", jobName)
            );
        }
    }

    private boolean isWorkflowExecutionJob(JobExecutionContext context) {
        return !WorkflowExecutionTask.class.equals(context.getJobDetail().getJobClass());
    }

    private void maybeRescheduleJob(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().toString();
        String caseHubInstanceUuid = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
        String workerId = context.getMergedJobDataMap().getString("workerId");

        RetryPolicy retryPolicy = findRetryPolicy(jobName, caseHubInstanceUuid, workerId);
        if (retryPolicy == null) {
            return;
        }

        String idempotency = context.getMergedJobDataMap().getString("idempotency");
        UUID caseId = UUID.fromString(caseHubInstanceUuid);

        countFailedAttempts(caseId, workerId, idempotency)
                .subscribe().with(
                        failureCount -> {
                            if (failureCount < retryPolicy.maxAttempts()) {
                                LOG.infof("Rescheduling worker %s: attempt %d/%d, delayMs=%d",
                                        workerId, failureCount + 1, retryPolicy.maxAttempts(), retryPolicy.delayMs());
                                rescheduleJob(context, retryPolicy);
                            } else {
                                LOG.warnf("Worker %s exhausted all %d retry attempts for case %s",
                                        workerId, retryPolicy.maxAttempts(), caseHubInstanceUuid);
                                eventBus.publish(EventBusAddresses.WORKER_RETRIES_EXHAUSTED, new WorkerRetriesExhaustedEvent(
                                        caseId,
                                        workerId,
                                        idempotency
                                ));
                            }
                        },
                        ex -> LOG.errorf(ex, "Failed to count failed attempts for worker %s", workerId)
                );
    }

    private RetryPolicy findRetryPolicy(String jobName, String caseHubInstanceUuid, String workerId) {
        CaseInstance instance = caseInstanceCache.get(UUID.fromString(caseHubInstanceUuid));
        if (instance == null) {
            LOG.errorf("Cannot reschedule job %s: CaseInstance not found for uuid=%s", jobName, caseHubInstanceUuid);
            return null;
        }

        CaseHubDefinition definition = caseDefinitionRegistry.getCaseDefinition(instance.getCaseDefinition());
        if (definition == null) {
            LOG.errorf("Cannot reschedule job %s: CaseHubDefinition not found", jobName);
            return null;
        }

        Worker worker = definition.getWorkers().stream()
                .filter(w -> w.getName().equals(workerId))
                .findFirst()
                .orElse(null);

        if (worker == null) {
            LOG.errorf("Cannot reschedule job %s: Worker not found: %s", jobName, workerId);
            return null;
        }

        ExecutionPolicy executionPolicy = worker.getExecutionPolicy();
        if (executionPolicy == null || executionPolicy.retries() == null) {
            LOG.infof("No retry policy configured for worker %s, skipping reschedule", workerId);
            return null;
        }

        return executionPolicy.retries();
    }

    private void rescheduleJob(JobExecutionContext context, RetryPolicy retryPolicy) {
        String idempotency = context.getMergedJobDataMap().getString("idempotency");
        String group = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
        JobKey jobKey = new JobKey(idempotency, group);

        JobDetail job = newJob(WorkflowExecutionTask.class)
                .withIdentity(jobKey)
                .storeDurably(false)
                .usingJobData(context.getMergedJobDataMap())
                .build();

        int delayMs = retryPolicy.delayMs() > 0 ? retryPolicy.delayMs() : 0;

        Trigger trigger = newTrigger()
                .withIdentity(idempotency, group)
                .startAt(new Date(System.currentTimeMillis() + delayMs))
                .forJob(jobKey)
                .build();

        workflowExecutionScheduler.scheduleOrRescheduleAsync(job, trigger)
                .subscribe().with(
                        ignored -> LOG.infof("Rescheduled job: %s", jobKey),
                        ex -> LOG.errorf(ex, "Failed to reschedule job: %s", jobKey)
                );
    }

    private static EventLog createEventLog(JobExecutionContext context, CaseHubEventType eventType, JsonNode metadata) {
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
        return runOnSafeVertxContext(() ->
                sessionFactory.withTransaction(session -> session.persist(eventLog))
        ).onFailure().invoke(ex ->
                LOG.errorf(ex, "Failed to persist and reschedule job: %s", jobName)
        ).replaceWithVoid();
    }

    //TODO metadata->>'idempotency' way faster but not very stable
    private Uni<Long> countFailedAttempts(UUID caseId, String workerId, String idempotency) {
        return runOnSafeVertxContext(() ->
                sessionFactory.withSession(session ->
                        session.createSelectionQuery(
                                        "from EventLog where caseId = :caseId and workerId = :workerId and eventType = :eventType",
                                        EventLog.class)
                                .setParameter("caseId", caseId)
                                .setParameter("workerId", workerId)
                                .setParameter("eventType", CaseHubEventType.WORKER_EXECUTION_FAILED)
                                .getResultList()
                                .map(eventLogs -> eventLogs.stream()
                                        .filter(eventLog -> {
                                            JsonNode metadata = eventLog.getMetadata();
                                            JsonNode idempotencyNode = metadata == null ? null : metadata.get("idempotency");
                                            return idempotencyNode != null && idempotency.equals(idempotencyNode.asText());
                                        })
                                        .count())
                )
        );
    }

    @SuppressWarnings("unchecked")
    private <T> Uni<T> runOnSafeVertxContext(Supplier<Uni<? extends T>> action) {
        return Uni.createFrom().<T>deferred(() -> (Uni<T>) action.get())
                .runSubscriptionOn(command -> {
                    Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);
                    VertxContextSafetyToggle.setContextSafe(dc, true);
                    dc.runOnContext(v -> command.run());
                });
    }
}
