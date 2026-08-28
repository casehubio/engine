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
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.event.EventStreamType;
import io.casehub.api.spi.judgment.VerificationResult;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.event.JudgmentEscalatedEvent;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.spi.EventLogRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import org.jboss.logging.Logger;

@ApplicationScoped
public class JudgmentEscalationHandler {

  private static final Logger LOG = Logger.getLogger(JudgmentEscalationHandler.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Inject EventLogRepository eventLogRepository;

  @ConsumeEvent(value = EventBusAddresses.JUDGMENT_ESCALATED)
  @RunOnVirtualThread
  public void onJudgmentEscalated(final JudgmentEscalatedEvent event) {
    final EventLog log = new EventLog();
    log.setCaseId(event.caseId());
    log.setStreamType(EventStreamType.CASE);
    log.setTimestamp(Instant.now());
    log.setEventType(CaseHubEventType.JUDGMENT_ESCALATED);
    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.put("bindingName", event.bindingName());
    metadata.put(
        "fromCallerId",
        event.originalResponse().callerId() != null
            ? event.originalResponse().callerId()
            : "unknown");
    metadata.put(
        "fromCallerType",
        event.originalResponse().callerType() != null
            ? event.originalResponse().callerType()
            : "unknown");
    String reason =
        switch (event.result()) {
          case VerificationResult.InsufficientEvidence ie ->
              "insufficient_evidence: " + ie.feedback();
          case VerificationResult.TrustTooLow ttl ->
              "trust_too_low: required=" + ttl.requiredLevel();
          default -> "unknown";
        };
    metadata.put("reason", reason);
    log.setMetadata(metadata);
    eventLogRepository.append(log, event.tenancyId());
    LOG.infof(
        "Judgment escalated: caseId=%s binding=%s reason=%s",
        event.caseId(), event.bindingName(), reason);
  }
}
