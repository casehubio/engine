package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@ApplicationScoped
public class WorkflowExecutionManager {

    @Inject
    Scheduler quartz;

    @Inject
    WorkflowExecutionJobListener workflowExecutionJobListener;

    @Inject
    WorkflowExecutionScheduler workflowExecutionScheduler;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = Logger.getLogger(WorkflowExecutionManager.class);


    void onStart(@Observes StartupEvent ev) throws SchedulerException {
        quartz.getListenerManager().addJobListener(workflowExecutionJobListener);
    }

    // TODO, yes, here is id to event object, because later it can be splitted into multiple jobs on diff jvms
    public Uni<Void> submit(Long eventLogId,
                            CaseInstance instance,
                            Worker worker,
                            Capability capability,
                            Map<String, Object> inputData) {

        String idempotency = worker.getName() + ":" + capability.getName() + ":" + computeInputDataHash(inputData);
        String group = instance.getUuid().toString();

        return Panache.withTransaction(() -> EventLog.<EventLog>findById(eventLogId)
                        .onItem().ifNull().failWith(() ->
                                new NotFoundException("EventLog not found: id=" + eventLogId))
                        .replaceWithVoid())
                .chain(() -> scheduleQuartzJob(eventLogId, instance, worker, idempotency, group));
    }

    private Uni<Void> scheduleQuartzJob(Long eventLogId, CaseInstance instance, Worker worker, String idempotency, String group) {
        JobKey jobKey = new JobKey(idempotency, group);
        JobDetail job = newJob(WorkflowExecutionTask.class)
                .withIdentity(jobKey)
                .storeDurably(false)
                .usingJobData("idempotency", idempotency)
                .usingJobData("caseHubInstanceUuid", instance.getUuid().toString())
                .usingJobData("workerId", worker.getName())
                .usingJobData("eventLogId", String.valueOf(eventLogId))
                .build();

        Trigger trigger = newTrigger()
                .withIdentity(idempotency, group)
                .startNow()
                .forJob(jobKey)
                .build();

        return workflowExecutionScheduler.scheduleOrRescheduleAsync(job, trigger);
    }

    // TODO: move to util class and add tests, also consider using a more efficient hashing approach for large input data (e.g. streaming JSON parser + incremental hashing)
    private String computeInputDataHash(Map<String, Object> inputData) {
        try {
            String json = OBJECT_MAPPER.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(inputData);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute input data hash", e);
        }
    }
}
