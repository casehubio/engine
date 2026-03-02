package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.WorkflowExecutionCompleted;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.model.Capability;
import io.casehub.model.CaseHubDefinition;
import io.casehub.model.Worker;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowModel;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.casehub.engine.internal.event.EventBusAddresses.WORKER_EXECUTION_FINISHED;

@SuppressWarnings("unchecked")


@ApplicationScoped
public class WorkflowExecutionTask implements Job {

  @Inject
  WorkflowExecutor workflowExecutor;

  @Inject
  CaseDefinitionRegistry caseDefinitionRegistry;


  @Inject
  CaseInstanceCache caseInstanceCache;

  @Inject
  Mutiny.SessionFactory sessionFactory;

  @Inject
  Vertx vertx;

  @Inject
  EventBus eventBus;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void execute(JobExecutionContext executionContext) throws JobExecutionException {
    System.out.println("Executing workflow task: " + executionContext.getJobDetail().getKey());
    Long eventLogId = Long.parseLong(executionContext.getMergedJobDataMap().getString("eventLogId"));
    String idempotency = executionContext.getMergedJobDataMap().getString("idempotency");

    EventLog eventLog = findEventLog(eventLogId);
    if (eventLog == null) {
      throw new JobExecutionException("EventLog not found: id=" + eventLogId);
    }

    Map<String, Object> inputData = OBJECT_MAPPER.convertValue(eventLog.getPayload(), Map.class);
    CaseInstance instance = caseInstanceCache.get(eventLog.getCaseId());
    CaseHubDefinition definition = caseDefinitionRegistry.getCaseDefinition(instance.getCaseDefinition());
    String workflowId = eventLog.getWorkerId();
    String capabilityName = eventLog.getMetadata().get("capabilityName").asText();

    //TODO use map
    Worker worker = definition.getSpec()
            .getWorkers()
            .stream()
            .filter(w -> w.getName().equals(workflowId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Worker not found in case definition: " + workflowId));

    //TODO use map
    Capability capability = definition.getSpec()
            .getCapabilities()
            .stream()
            .filter(c -> c.getName().equals(capabilityName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Capability not found in case definition: " + capabilityName));


    Workflow workflow = resolveWorkflow(worker.getWorkflow());
    CompletableFuture<WorkflowModel> cf = workflowExecutor.execute(workflow, inputData);
    WorkflowModel workflowModel = cf.join(); //TODO handle exception + join() in a non-blocking way
    Map<String, Object> outputData = workflowModel.asMap().orElseThrow(() -> new RuntimeException("Failed to convert workflow model to map"));

    Map<String, Object> toContextOutputData = new StateContextImpl(outputData).evalObjectTemplate(capability.getOutputSchema());

    WorkflowExecutionCompleted event = new WorkflowExecutionCompleted(
            instance,
            worker,
            idempotency,
            toContextOutputData
    );
    eventBus.publish(WORKER_EXECUTION_FINISHED, event);
  }

  private EventLog findEventLog(Long eventLogId) throws JobExecutionException {
    try {
      return sessionFactory
              .withSession(s -> s.find(EventLog.class, eventLogId))
              .runSubscriptionOn(command -> {
                Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);
                VertxContextSafetyToggle.setContextSafe(dc, true);
                dc.runOnContext(v -> command.run());
              })
              .await().atMost(Duration.ofSeconds(10));
    } catch (Exception e) {
      throw new JobExecutionException("Failed to load EventLog id=" + eventLogId, e);
    }
  }

  private Workflow resolveWorkflow(Object asObject) {
    if (asObject instanceof Workflow workflow) {
      return workflow;
    } else if (asObject instanceof String path) {
      throw new RuntimeException("Workflow definition as file path is not supported yet: " + path);
    }
    throw new RuntimeException("Unsupported workflow definition format: " + asObject);
  }

}
