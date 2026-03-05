package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.engine.CaseDefinitionRegistry;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.worker.WorkflowExecutionManager;
import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class WorkerScheduleEventHandler {

  private static final Logger LOG = Logger.getLogger(CaseStartedEventHandler.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  CaseDefinitionRegistry caseDefinitionRegistry;

  @Inject
  WorkflowExecutionManager workflowExecutionManager; //TODO refactor to publish events instead of calling manager directly


  @ConsumeEvent(value = EventBusAddresses.SCHEDULE_WORKER)
  public Uni<Void> onWorkerScheduleEventHandler(WorkerScheduleEvent event) {
    CaseInstance instance = event.caseInstance();
    Worker worker = event.worker();
    Capability capability = event.capability();

    Map<String, Object> inputData = instance.getStateContext().evalObjectTemplate(capability.getInputSchema());
    String inputDataHash = computeInputDataHash(inputData);

    Map<String, String> metadata = Map.of(
            "workerName", worker.getName(),
            "capabilityName", capability.getName(),
            "inputDataHash", inputDataHash
    );

    JsonNode metadataJsonNode = OBJECT_MAPPER.valueToTree(metadata);
    JsonNode payloadJsonNode = OBJECT_MAPPER.valueToTree(inputData);

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(instance.getUuid());
    eventLog.setEventType(CaseHubEventType.WORKER_SCHEDULED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setWorkerId(worker.getName());
    eventLog.setMetadata(metadataJsonNode);
    eventLog.setPayload(payloadJsonNode);

    return Panache.withTransaction(() ->
                    eventLog.persistAndFlush()
                            .map(persisted -> ((EventLog) persisted).id)
            )
            .call(eventLogId ->
                    workflowExecutionManager.submit(eventLogId, instance, worker, capability, inputData)
            )
            .invoke(() -> LOG.infof(
                    "WorkerScheduleEvent processed: caseId=%s worker=%s capability=%s",
                    instance.getUuid(), worker.getName(), capability.getName()
            ))
            .replaceWithVoid()
            .onFailure().invoke(t -> LOG.errorf(t,
                    "WorkerScheduleEvent FAILED: caseId=%s worker=%s capability=%s",
                    instance.getUuid(), worker.getName(), capability.getName()
            ));
  }

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
