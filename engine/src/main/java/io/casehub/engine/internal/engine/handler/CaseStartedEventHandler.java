package io.casehub.engine.internal.engine.handler;

import io.casehub.engine.internal.event.CaseStartedEvent;
import io.casehub.engine.internal.event.CaseStateContextChangedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;

@ApplicationScoped
public class CaseStartedEventHandler {

  private static final Logger LOG = Logger.getLogger(CaseStartedEventHandler.class);

  @Inject
  EventBus eventBus;

  @ConsumeEvent(value = EventBusAddresses.CASE_STARTED)
  public Uni<Void> onCaseStarted(CaseStartedEvent event) {
    CaseInstance instance = event.instance();
    EventLog eventLog = new EventLog();
    eventLog.setCaseId(instance.getUuid());
    eventLog.setEventType(CaseHubEventType.CASE_STARTED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now()); // replace with @PrePersist in EventLog
    eventLog.setPayload(instance.getStateContext().asJsonNode());

    return Panache.withTransaction(() -> eventLog.persist()
            .invoke(() -> eventBus.publish(EventBusAddresses.CONTEXT_CHANGED, new CaseStateContextChangedEvent(instance)))
            .replaceWithVoid());
  }
}
