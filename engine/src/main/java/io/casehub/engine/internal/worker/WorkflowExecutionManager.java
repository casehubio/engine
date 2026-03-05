package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.CaseHubReactor;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@ApplicationScoped
public class WorkflowExecutionManager {

  @Inject
  WorkflowExecutor workflowExecutor;

  @Inject
  CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject
  Scheduler quartz;

  @Inject
  Mutiny.SessionFactory sessionFactory;

  @Inject
  Vertx vertx;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Logger LOG = Logger.getLogger(CaseHubReactor.class);


  void onStart(@Observes StartupEvent ev) throws SchedulerException {
    quartz.getListenerManager().addJobListener(new JobListener() {

      @Override
      public String getName() {
        return WorkflowExecutionManager.class.getSimpleName();
      }

      @Override
      public void jobToBeExecuted(JobExecutionContext context) {
        EventLog eventLog = new EventLog();

        String jobName = context.getJobDetail().getKey().toString();
        String idempotency = context.getMergedJobDataMap().getString("idempotency");
        String caseHubInstanceUuid = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
        String workerId = context.getMergedJobDataMap().getString("workerId");
        System.out.println("📋 Job is about to be executed: " + jobName + ", idempotency=" + idempotency + ", caseHubInstanceUuid=" + caseHubInstanceUuid + ", workerId=" + workerId);

        eventLog.setCaseId(UUID.fromString(caseHubInstanceUuid));
        eventLog.setWorkerId(workerId);
        eventLog.setEventType(CaseHubEventType.WORKER_EXECUTION_STARTED);
        eventLog.setStreamType(EventStreamType.CASE);
        eventLog.setTimestamp(context.getFireTime().toInstant());
        eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
                .put("idempotency", idempotency)
        );

        persistEventLog(jobName, eventLog);
      }

      @Override
      public void jobExecutionVetoed(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().toString();
        LOG.info("Job execution was vetoed for job: " + jobName);
      }

      @Override
      public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (jobException != null) {
          String jobName = context.getJobDetail().getKey().toString();
          System.out.println("❌ Job failed: " + jobName + ", Error: " + jobException.getMessage());
          String idempotency = context.getMergedJobDataMap().getString("idempotency");
          String caseHubInstanceUuid = context.getMergedJobDataMap().getString("caseHubInstanceUuid");
          String workerId = context.getMergedJobDataMap().getString("workerId");

          EventLog eventLog = new EventLog();
          eventLog.setCaseId(UUID.fromString(caseHubInstanceUuid));
          eventLog.setWorkerId(workerId);
          eventLog.setStreamType(EventStreamType.CASE);
          eventLog.setTimestamp(context.getFireTime().toInstant());
          eventLog.setEventType(CaseHubEventType.WORKER_EXECUTION_FAILED);
          eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
                  .put("idempotency", idempotency)
                  .put("errorMessage", jobException.getMessage()));
          persistEventLog(jobName, eventLog);

          //reschedule the job for retry, here we can also implement an exponential backoff strategy based on the number of retries, for simplicity we just reschedule it to run immediately

        }
      }
    });
  }

  private void persistEventLog(String jobName, EventLog eventLog) {
    sessionFactory.withTransaction(session ->
            session.persist(eventLog)
    ).runSubscriptionOn(command -> {
      Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);
      VertxContextSafetyToggle.setContextSafe(dc, true);
      dc.runOnContext(v -> command.run());
    }).subscribe().with(
            success -> {
            },
            ex -> LOG.error("Failed to persist job failure log for job: " + jobName, ex)
    );
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
    return Uni.createFrom().item(() -> {
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

              try {
                if (quartz.checkExists(jobKey)) {
                  quartz.rescheduleJob(new TriggerKey(idempotency, group), trigger);
                  return null;
                }
                quartz.scheduleJob(job, trigger);
                return (Void) null;
              } catch (SchedulerException e) {
                throw new RuntimeException("Quartz scheduling failed for jobKey=" + jobKey, e);
              }
            })
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
            .replaceWithVoid();
  }

  // TODO: move to util class and add tests, also consider using a more efficient hashing approach for large input data (e.g. streaming JSON parser + incremental hashing)
  private String computeInputDataHash(Map<String, Object> inputData) {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(inputData);

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
