package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.MilestoneReachedEvent;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.model.Milestone;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

import static io.casehub.engine.internal.history.CaseHubEventType.MILESTONE_REACHED;

@ApplicationScoped
public class MilestoneReachedEventHandler {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ConsumeEvent(value = EventBusAddresses.MILESTONE_REACHED)
  public Uni<Void> onMilestoneReachedEventHandler(MilestoneReachedEvent event) {
    CaseInstance caseInstance = event.caseInstance();
    Milestone milestone = event.milestone();

    EventLog eventLog = new EventLog();
    eventLog.setCaseId(caseInstance.getUuid());
    eventLog.setEventType(MILESTONE_REACHED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setMetadata(OBJECT_MAPPER.createObjectNode()
            .put("name", milestone.getName())
            .put("description", milestone.getDescription())
    );

    return Panache.withTransaction(eventLog::persist)
            .replaceWithVoid();
  }
}
