package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.event.CaseStatusChanged;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseState;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

@ApplicationScoped
public class CaseStatusChangedHandler {

  private static final Logger LOG = Logger.getLogger(CaseStatusChangedHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject
  EventBus eventBus;

  @ConsumeEvent(value = EventBusAddresses.CASE_STATUS_CHANGED)
  public Uni<Void> onCaseStatusChangedHandler(CaseStatusChanged event) {
    CaseInstance caseInstance = event.instance();
    CaseState newState = CaseState.valueOf(event.newStatus());
    String oldStatus = event.oldStatus();

    LOG.infof("Case status changed: caseId=%s, %s -> %s", caseInstance.getUuid(), oldStatus, event.newStatus());

    CaseHubEventType eventType = resolveState(newState);

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(caseInstance.getUuid());
    eventLog.setEventType(eventType);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
            .put("oldStatus", oldStatus)
            .put("newStatus", event.newStatus())
    );

    caseInstance.setState(newState);

    return Panache.withTransaction(() ->
            Panache.getSession()
                    .chain(session -> session.merge(caseInstance))
                    .chain(merged -> eventLog.persist())
    ).invoke(() -> {
      String eventBusAddress = resolveStateAsString(newState);
      if (eventBusAddress != null) {
        eventBus.publish(eventBusAddress, caseInstance);
      }
    }).replaceWithVoid();
  }

  private CaseHubEventType resolveState(CaseState state) {
    return switch (state) {
      case COMPLETED -> CaseHubEventType.CASE_COMPLETED;
      case FAILED -> CaseHubEventType.CASE_FAILED;
      default -> CaseHubEventType.CASE_STATUS_CHANGED;
    };
  }

  private String resolveStateAsString(CaseState state) {
    return switch (state) {
      case COMPLETED -> EventBusAddresses.CASE_COMPLETED;
      case FAILED -> EventBusAddresses.CASE_FAILED;
      default -> null;
    };
  }
}
