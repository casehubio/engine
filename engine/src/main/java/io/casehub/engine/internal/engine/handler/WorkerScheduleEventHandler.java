package io.casehub.engine.internal.engine.handler;

import io.casehub.context.StateContext;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerRunEvent;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.model.Capability;
import io.casehub.model.Worker;
import io.quarkus.vertx.ConsumeEvent;
import io.serverlessworkflow.api.types.Workflow;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class WorkerScheduleEventHandler {

  private static final Logger LOG = Logger.getLogger(WorkerScheduleEventHandler.class);

  @Inject
  EventBus eventBus;

  @ConsumeEvent(value = EventBusAddresses.SCHEDULE_WORKER_RUN)
  public Uni<Void> handleScheduleWorkerRun(WorkerScheduleEvent event) {
    LOG.infof("=== WorkerScheduleEvent received ===");
    LOG.infof("Worker: %s", event.worker().getName());
    LOG.infof("Capability: %s", event.capability().getName());

    return Uni.createFrom()
            .item(() -> {
              Worker worker = event.worker();
              StateContext stateContext = event.runState().getStateContext();
              Capability capability = event.capability();

              LOG.debugf("INPUT Schema: %s", capability.getInputSchema());
              LOG.debugf("OUTPUT Schema: %s", capability.getOutputSchema());

              Map<String, Object> inputData = stateContext.evalObjectTemplate(capability.getInputSchema());
              LOG.infof("Evaluated input data with %d keys: %s", inputData.size(), inputData.keySet());

              Workflow workflow;
              if (worker.isWorkflowRef()) {
                String workflowRef = worker.getWorkflowAsRef();
                LOG.infof("Worker uses external workflow reference: %s", workflowRef);
                // TODO: Load workflow from external file
                throw new UnsupportedOperationException(
                    "External workflow references not yet implemented. WorkflowRef: " + workflowRef);
              } else if (worker.isEmbeddedWorkflow()) {
                LOG.infof("Worker uses embedded workflow definition");
                workflow = worker.getWorkflowAsEmbedded();
              } else {
                throw new IllegalStateException("Worker has no workflow definition: " + worker.getName());
              }

              WorkerRunEvent workerRunEvent = new WorkerRunEvent(
                      event.runState(),
                      worker,
                      capability,
                      workflow,
                      inputData
              );

              eventBus.publish(EventBusAddresses.WORKER_RUN, workerRunEvent);
              LOG.infof("Published WorkerRunEvent for worker '%s' with capability '%s'",
                      worker.getName(), capability.getName());
              LOG.infof("================================");

              return null;
            })
            .replaceWithVoid();
  }
}
