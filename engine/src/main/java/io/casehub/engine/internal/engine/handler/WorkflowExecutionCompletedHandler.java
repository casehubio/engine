package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.event.CaseStateContextChangedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkflowExecutionCompleted;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.model.Worker;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class WorkflowExecutionCompletedHandler {

  @Inject
  EventBus eventBus;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final Logger LOG = Logger.getLogger(CaseStartedEventHandler.class);

  @ConsumeEvent(value = EventBusAddresses.WORKER_EXECUTION_FINISHED)
  public Uni<Void> onWorkflowExecutionCompletedHandler(WorkflowExecutionCompleted event) {
    final CaseInstance caseInstance = event.caseInstance();
    final Worker worker = event.worker();
    final Map<String, Object> rawOutput = event.output() == null ? Map.of() : event.output();
    final Instant now = Instant.now();

    final EventLog eventLog = buildEventLog(caseInstance, worker, rawOutput, event.idempotency(), now);
    return Panache.withTransaction(() -> {
              caseInstance.getStateContext().setAll(rawOutput);
              return eventLog.persist();
            })
            .invoke(() -> eventBus.publish(
                    EventBusAddresses.CONTEXT_CHANGED,
                    new CaseStateContextChangedEvent(caseInstance)
            ))
            .replaceWithVoid()
            .onFailure().invoke(t -> {
                LOG.error("Failed to handle WorkflowExecutionCompleted event for caseId: " + caseInstance.getUuid(), t);
            });
  }

  private EventLog buildEventLog(
          CaseInstance caseInstance,
          Worker worker,
          Map<String, Object> output,
          String idempotency,
          Instant timestamp
  ) {
    final EventLog eventLog = new EventLog();
    eventLog.setCaseId(caseInstance.getUuid());
    eventLog.setWorkerId(worker.getName());
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(timestamp);
    eventLog.setEventType(CaseHubEventType.WORKER_EXECUTION_COMPLETED);

    eventLog.setPayload(OBJECT_MAPPER.valueToTree(output == null ? Map.of() : output));

    eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
            .put("idempotency", idempotency)
    );

    return eventLog;
  }
}
