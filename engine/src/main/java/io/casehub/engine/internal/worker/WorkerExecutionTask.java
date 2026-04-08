package io.casehub.engine.internal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.context.StateContextImpl;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.engine.recovery.WorkerExecutionRecoveryService;
import io.casehub.engine.internal.event.WorkflowExecutionCompleted;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.ReactiveUtils;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowModel;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
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
public class WorkerExecutionTask implements Job {

    @Inject
    WorkflowExecutor workflowExecutor;

    @Inject
    CaseDefinitionRegistry caseDefinitionRegistry;

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @Inject
    WorkerExecutionRecoveryService workerExecutionRecoveryService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = Logger.getLogger(WorkerExecutionTask.class);

    @Override
    public void execute(JobExecutionContext executionContext) throws JobExecutionException {
        LOG.infof("Executing workflow task: %s", executionContext.getJobDetail().getKey());

        String inputDataHash = executionContext.getMergedJobDataMap().getString("inputDataHash");
        String eventLogId = executionContext.getMergedJobDataMap().getString("eventLogId");

        execute(inputDataHash, eventLogId);
    }

    private void execute(String inputDataHash, String eventLogId) throws JobExecutionException {
        EventLog eventLog = findEventLog(eventLogId)
                .subscribe()
                .asCompletionStage()
                .toCompletableFuture()
                .join(); //TODO

        if (eventLog == null) {
            throw new JobExecutionException("EventLog not found: id=" + eventLogId);
        }

        Map<String, Object> inputData = OBJECT_MAPPER.convertValue(eventLog.getPayload(), Map.class);

        //TODO
        CaseInstance instance = workerExecutionRecoveryService.loadOrRestoreCaseInstance(eventLog.getCaseId())
                .await().atMost(Duration.ofSeconds(10));

        CaseHubDefinition definition = caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());

        if (definition == null) {
            throw new JobExecutionException("CaseHubDefinition not found for caseId=" + eventLog.getCaseId());
        }
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
        if (worker.getFunction().getValue() instanceof Workflow workflow) {
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
                inputDataHash,
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

    private Uni<EventLog> findEventLog(String eventLogId) {
        return ReactiveUtils.runOnSafeVertxContext(vertx, () ->
                Panache.withSession(() -> EventLog.findById(eventLogId))
        );
    }

}
