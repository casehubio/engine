package io.casehub.engine.internal.worker;

import io.casehub.engine.internal.engine.recovery.WorkerExecutionRecoveryService;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.WorkerExecutionKeys;
import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import java.time.Duration;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@ApplicationScoped
public class WorkerExecutionManager {

    @Inject
    Scheduler quartz;

    @Inject
    WorkerExecutionJobListener workflowExecutionJobListener;

    @Inject
    WorkerExecutionScheduler workerExecutionScheduler;

    @Inject
    WorkerExecutionRecoveryService workerExecutionRecoveryService;

    private static final Logger LOG = Logger.getLogger(WorkerExecutionManager.class);

    //TODO this must be reworked
    void onStart(@Observes @Priority(20) StartupEvent ev) throws SchedulerException {
        quartz.getListenerManager().addJobListener(workflowExecutionJobListener);

        // TODO fix it
        workerExecutionRecoveryService.recoverPendingScheduledWorkers()
                .await().atMost(Duration.ofSeconds(30));
    }

    // TODO, yes, here is id of  event object, because later it can be splitted into multiple jobs on diff jvms
    public Uni<Void> submit(Long eventLogId,
                            CaseInstance instance,
                            Worker worker,
                            Capability capability,
                            Map<String, Object> inputData) {

        String idempotency = WorkerExecutionKeys.inputDataHash(
                worker.getName(),
                capability.getName(),
                inputData
        );
        String group = instance.getUuid().toString();

        return Panache.withTransaction(() -> EventLog.<EventLog>findById(eventLogId)
                        .onItem().ifNull().failWith(() ->
                                new NotFoundException("EventLog not found: id=" + eventLogId))
                        .replaceWithVoid())
                .chain(() -> scheduleQuartzJob(eventLogId, instance, worker, idempotency, group));
    }

    private Uni<Void> scheduleQuartzJob(Long eventLogId, CaseInstance instance, Worker worker, String idempotency, String group) {
        return scheduleQuartzJob(eventLogId, instance.getUuid().toString(), worker.getName(), idempotency, group);
    }

    public Uni<Void> schedulePersistedEvent(EventLog scheduledEventLog) {
        String caseId = scheduledEventLog.getCaseId().toString();
        String workerId = scheduledEventLog.getWorkerId();
        String idempotency = scheduledEventLog.getMetadata().get("inputDataHash").asText();
        return scheduleQuartzJob(scheduledEventLog.id, caseId, workerId, idempotency, caseId);
    }

    private Uni<Void> scheduleQuartzJob(Long eventLogId,
                                        String caseHubInstanceUuid,
                                        String workerId,
                                        String idempotency,
                                        String group) {
        JobKey jobKey = new JobKey(idempotency, group);
        JobDetail job = newJob(WorkerExecutionTask.class)
                .withIdentity(jobKey)
                .storeDurably(false)
                .usingJobData("inputDataHash", idempotency)
                .usingJobData("caseHubInstanceUuid", caseHubInstanceUuid)
                .usingJobData("workerId", workerId)
                .usingJobData("eventLogId", String.valueOf(eventLogId))
                .build();

        Trigger trigger = newTrigger()
                .withIdentity(idempotency, group)
                .startNow()
                .forJob(jobKey)
                .build();

        return workerExecutionScheduler.scheduleOrRescheduleAsync(job, trigger);
    }
}
