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

import static io.casehub.engine.internal.event.EventBusAddresses.CONTEXT_CHANGED;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.context.StateContext;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.engine.recovery.WorkerExecutionRecoveryService;
import io.casehub.engine.internal.event.CaseStateContextChangedEvent;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.SignalReceivedEvent;
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
public class SignalReceivedEventHandler {

  private static final Logger LOG = Logger.getLogger(SignalReceivedEventHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject EventBus eventBus;

  @Inject CaseInstanceCache caseInstanceCache;

  @Inject WorkerExecutionRecoveryService recoveryService;

  @ConsumeEvent(value = EventBusAddresses.SIGNAL_RECEIVED)
  public Uni<Void> onSignalReceived(SignalReceivedEvent event) {
    CaseInstance cached = caseInstanceCache.get(event.caseId());
    if (cached != null) {
      return applySignal(cached, event);
    }
    LOG.warnf("CaseInstance not found in cache for caseId=%s, trying recovery", event.caseId());
    return recoveryService
        .loadOrRestoreCaseInstance(event.caseId())
        .chain(instance -> applySignal(instance, event));
  }

  private Uni<Void> applySignal(CaseInstance instance, SignalReceivedEvent event) {
    StateContext before = instance.getStateContext().snapshot();
    instance.getStateContext().setPath(event.path(), event.value());
    JsonNode diff = before.diff(instance.getStateContext());
    JsonNode contextSnapshot = instance.getStateContext().asJsonNode();

    if (diff.isEmpty()) {
      LOG.debugf(
          "Signal path='%s' produced no state change for caseId=%s — skipping",
          event.path(), event.caseId());
      return Uni.createFrom().voidItem();
    }

    return Panache.withTransaction(
            () -> {
              EventLog eventLog = buildSignalEventLog(instance, diff);
              return eventLog
                  .persist()
                  .invoke(
                      () ->
                          eventBus.publish(
                              CONTEXT_CHANGED,
                              new CaseStateContextChangedEvent(instance, contextSnapshot)));
            })
        .replaceWithVoid()
        .onFailure()
        .invoke(
            t ->
                LOG.errorf(
                    t,
                    "Failed to process signal path='%s' for caseId=%s",
                    event.path(),
                    event.caseId()));
  }

  private EventLog buildSignalEventLog(CaseInstance instance, JsonNode diff) {
    EventLog eventLog = new EventLog();
    eventLog.setCaseId(instance.getUuid());
    eventLog.setEventType(CaseHubEventType.SIGNAL_RECEIVED);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setPayload(OBJECT_MAPPER.createObjectNode().set("patch", diff.deepCopy()));
    return eventLog;
  }
}
