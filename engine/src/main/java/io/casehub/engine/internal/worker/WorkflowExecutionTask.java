package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.WorkflowExecutionCompleted;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
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
import java.util.function.Function;

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
    Worker worker = definition
            .getWorkers()
            .stream()
            .filter(w -> w.getName().equals(workflowId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Worker not found in case definition: " + workflowId));

    //TODO use map
    Capability capability = definition
            .getCapabilities()
            .stream()
            .filter(c -> c.getName().equals(capabilityName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Capability not found in case definition: " + capabilityName));

    Map<String, Object> outputData;
    if (worker.getFunction().getValue() instanceof Workflow workflow ) {
      outputData = workflow(workflow, inputData);
    } else if (worker.getFunction().getValue() instanceof Function function) {
      outputData = function(function, inputData);
    } else {
      throw new RuntimeException("Worker function is not a workflow: " + worker.getName() + " " + worker.getFunction().getValue().getClass().getCanonicalName());
    }

    Map<String, Object> toContextOutputData = new StateContextImpl(outputData).evalObjectTemplate(capability.getOutputSchema());

    WorkflowExecutionCompleted event = new WorkflowExecutionCompleted(
            instance,
            worker,
            idempotency,
            toContextOutputData
    );
    eventBus.publish(WORKER_EXECUTION_FINISHED, event);
  }

  private Map<String, Object> workflow(Workflow workflow, Map<String, Object> inputData) {
    CompletableFuture<WorkflowModel> cf = workflowExecutor.execute(workflow, inputData);
    WorkflowModel workflowModel = cf.join(); //TODO handle exception + join() in a non-blocking way
    return workflowModel.asMap().orElseThrow(() -> new RuntimeException("Failed to convert workflow model to map"));
  }

  private Map<String, Object> function(Function<Map<String, Object>, Map<String, Object>> function, Map<String, Object> inputData) {
    return function.apply(inputData);
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

}
