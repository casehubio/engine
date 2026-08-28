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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.event.EventStreamType;
import io.casehub.engine.common.internal.event.CaseContextChangedEvent;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.event.JudgmentCompletedEvent;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JudgmentCompletedHandler {

  private static final Logger LOG = Logger.getLogger(JudgmentCompletedHandler.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Inject CaseInstanceCache caseInstanceCache;
  @Inject CaseDefinitionRegistry caseDefinitionRegistry;
  @Inject EventLogRepository eventLogRepository;
  @Inject EventBus eventBus;

  @ConsumeEvent(value = EventBusAddresses.JUDGMENT_COMPLETED)
  @RunOnVirtualThread
  public void onJudgmentCompleted(final JudgmentCompletedEvent event) {
    final CaseInstance instance = caseInstanceCache.get(event.caseId());
    if (instance == null) {
      LOG.warnf(
          "CaseInstance not in cache for judgment completion: caseId=%s — discarding",
          event.caseId());
      return;
    }
    if (isTerminal(instance.getState())) {
      LOG.warnf(
          "Judgment response on terminated case (state=%s): caseId=%s — discarding",
          instance.getState(), event.caseId());
      return;
    }

    final CaseDefinition def =
        caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());
    if (def == null) {
      LOG.warnf(
          "CaseDefinition not found for caseId=%s — discarding judgment response", event.caseId());
      return;
    }

    Binding binding =
        def.getBindings().stream()
            .filter(b -> b.getName().equals(event.bindingName()))
            .findFirst()
            .orElse(null);
    if (binding != null
        && binding.target() instanceof JudgmentTarget jt
        && jt.outputMapping() != null) {
      Map<String, Object> responseData = new HashMap<>();
      responseData.put("decision", event.response().decision());
      responseData.put("evidence", event.response().evidence());
      instance.getCaseContext().set(event.bindingName(), responseData);
    }

    writeRespondedEventLog(instance, event);

    eventBus.publish(
        EventBusAddresses.CONTEXT_CHANGED,
        new CaseContextChangedEvent(instance, instance.getCaseContext(), "working"));

    LOG.infof(
        "Judgment response applied: caseId=%s binding=%s decision=%s",
        event.caseId(), event.bindingName(), event.response().decision());
  }

  private void writeRespondedEventLog(CaseInstance instance, JudgmentCompletedEvent event) {
    final EventLog log = new EventLog();
    log.setCaseId(instance.getUuid());
    log.setStreamType(EventStreamType.CASE);
    log.setTimestamp(Instant.now());
    log.setEventType(CaseHubEventType.JUDGMENT_RESPONDED);
    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.put("bindingName", event.bindingName());
    metadata.put("decision", event.response().decision());
    metadata.set("evidence", MAPPER.valueToTree(event.response().evidence()));
    if (event.response().callerId() != null) metadata.put("callerId", event.response().callerId());
    if (event.response().callerType() != null)
      metadata.put("callerType", event.response().callerType());
    log.setMetadata(metadata);
    eventLogRepository.append(log, instance.tenancyId);
  }

  private static boolean isTerminal(CaseStatus state) {
    return state == CaseStatus.COMPLETED
        || state == CaseStatus.FAULTED
        || state == CaseStatus.CANCELLED;
  }
}
