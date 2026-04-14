/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.engine.internal.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.Instant;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CaseStartedEventHandler {

  private static final Logger LOG = Logger.getLogger(CaseStartedEventHandler.class);

  @Inject EventBus eventBus;

  @ConsumeEvent(value = EventBusAddresses.CASE_STARTED)
  public Uni<Void> onCaseStarted(CaseStartedEvent event) {
    CaseInstance instance = event.instance();
    JsonNode contextSnapshot = instance.getStateContext().asJsonNode();
    EventLog eventLog = new EventLog();
    eventLog.setCaseId(instance.getUuid());
    eventLog.setEventType(CaseHubEventType.CASE_STARTED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now()); // replace with @PrePersist in EventLog
    eventLog.setPayload(contextSnapshot);

    return Panache.withTransaction(() -> eventLog.persist().replaceWithVoid())
        .invoke(
            () ->
                eventBus.publish(
                    EventBusAddresses.CONTEXT_CHANGED,
                    new CaseStateContextChangedEvent(instance, contextSnapshot)));
  }
}
