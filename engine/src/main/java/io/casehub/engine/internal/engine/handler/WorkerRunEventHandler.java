package io.casehub.engine.internal.engine.handler;

import io.casehub.context.StateContext;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.StateContextChangedEvent;
import io.casehub.engine.internal.event.WorkerRunEvent;
import io.casehub.engine.internal.worker.WorkflowExecutor;
import io.quarkus.vertx.ConsumeEvent;
import io.serverlessworkflow.api.types.Workflow;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class WorkerRunEventHandler {

  private static final Logger LOG = Logger.getLogger(WorkerRunEventHandler.class);

  @Inject
  EventBus eventBus;

  @Inject
  WorkflowExecutor workflowExecutor;

  @ConsumeEvent(value = EventBusAddresses.WORKER_RUN)
  public Uni<Void> onWorkerRun(WorkerRunEvent event) {
    LOG.infof("=== WorkerRunEvent received ===");
    LOG.infof("Execution ID: %s", event.getExecutionId());
    LOG.infof("Run ID: %s", event.getRunId());
    LOG.infof("Worker: %s", event.getWorker().getName());
    LOG.infof("Capability: %s", event.getCapability().getName());
    LOG.infof("Idempotency Key: %s", event.getIdempotencyKey());

    Workflow workflow = event.getWorkflow();
    Map<String, Object> inputData = event.getInputData();

    LOG.infof("Workflow configuration:");
    LOG.infof("  - Document: %s", workflow.getDocument() != null ? "present" : "not set");
    LOG.infof("  - Input: %s", workflow.getInput() != null ? "present" : "not set");
    LOG.infof("  - Output: %s", workflow.getOutput() != null ? "present" : "not set");
    LOG.infof("  - Do tasks: %s", workflow.getDo() != null ? workflow.getDo().size() + " tasks" : "not set");
    LOG.infof("  - Use: %s", workflow.getUse() != null ? "present" : "not set");
    LOG.infof("  - Schedule: %s", workflow.getSchedule() != null ? "present" : "not set");
    LOG.infof("  - Timeout: %s", workflow.getTimeout() != null ? "present" : "not set");

    LOG.infof("Input data: %s", inputData);

    LOG.infof("Starting workflow execution for worker '%s' with capability '%s'",
            event.getWorker().getName(),
            event.getCapability().getName());

    return Uni.createFrom().completionStage(workflowExecutor.execute(workflow, inputData))
            .onItem().transform(model -> {
              Map<String, Object> workflowResult = model.asMap().orElse(Map.of());
              LOG.infof("Workflow execution completed with result: %s", workflowResult);

              StateContext stateContext = event.getRunState().getStateContext();
              if (!workflowResult.isEmpty()) {
                LOG.infof("Updating StateContext with %d keys from workflow result", workflowResult.size());
                stateContext.setAll(workflowResult);
                LOG.infof("StateContext updated. New version: %d, size: %d",
                        stateContext.getVersion(), stateContext.size());

                StateContextChangedEvent contextChangedEvent = new StateContextChangedEvent(
                        event.getRunState(),
                        "WORKER_COMPLETED"
                );
                eventBus.publish(EventBusAddresses.CONTEXT_CHANGED, contextChangedEvent);
                LOG.infof("Published StateContextChangedEvent for worker '%s' completion",
                        event.getWorker().getName());
              } else {
                LOG.warnf("Workflow completed but returned empty result for worker '%s'",
                        event.getWorker().getName());
              }

              LOG.infof("================================");
              return null;
            })
            .replaceWithVoid()
            .onFailure().invoke(e ->
                    LOG.errorf(e, "Failed to execute workflow for worker '%s' with capability '%s'",
                            event.getWorker().getName(),
                            event.getCapability().getName())
            );
  }
}
