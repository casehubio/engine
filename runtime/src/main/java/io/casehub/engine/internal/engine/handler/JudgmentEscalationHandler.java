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
  private static final int DEFAULT_MAX_ESCALATIONS = 3;

  @Inject EventLogRepository eventLogRepository;
  @Inject io.casehub.engine.common.spi.CaseDefinitionRegistry caseDefinitionRegistry;
  @Inject io.casehub.platform.api.routing.StrategyResolver strategyResolver;
  @Inject io.vertx.mutiny.core.eventbus.EventBus eventBus;
  @Inject io.casehub.engine.common.internal.judgment.JudgmentNodeExecutor judgmentNodeExecutor;

  @ConsumeEvent(value = EventBusAddresses.JUDGMENT_ESCALATED)
  @RunOnVirtualThread
  public void onJudgmentEscalated(final JudgmentEscalatedEvent event) {
    io.casehub.api.model.Binding binding = findBinding(event.bindingName());
    io.casehub.api.model.JudgmentTarget target =
        binding != null && binding.target() instanceof io.casehub.api.model.JudgmentTarget jt
            ? jt
            : null;
    io.casehub.api.model.CaseDefinition definition = findDefinition(event.bindingName());

    int escalationCount =
        countPriorEscalations(event.caseId(), event.bindingName(), event.tenancyId());
    int maxEscalations =
        definition != null && definition.getMaxEscalations() != null
            ? definition.getMaxEscalations()
            : DEFAULT_MAX_ESCALATIONS;

    String escalatorId =
        target != null && target.escalatorStrategy() != null ? target.escalatorStrategy() : "fault";
    io.casehub.api.spi.judgment.JudgmentEscalator escalator =
        strategyResolver.resolve(io.casehub.api.spi.judgment.JudgmentEscalator.class, escalatorId);

    io.casehub.api.spi.judgment.EscalationContext ctx =
        new io.casehub.api.spi.judgment.EscalationContext(
            event.caseId(),
            event.tenancyId(),
            event.bindingName(),
            target,
            event.originalResponse().decision(),
            event.originalResponse().evidence(),
            event.originalResponse().callerId(),
            event.originalResponse().callerType(),
            event.result(),
            escalationCount,
            maxEscalations,
            definition);

    io.casehub.api.spi.judgment.EscalationDecision decision = escalator.escalate(ctx);

    writeEscalationEventLog(event, decision);

    switch (decision) {
      case io.casehub.api.spi.judgment.EscalationDecision.ReYield ry -> {
        LOG.infof(
            "Judgment re-yield: caseId=%s binding=%s feedback=%s",
            event.caseId(), event.bindingName(), ry.feedback());
        eventBus.publish(
            EventBusAddresses.JUDGMENT_RE_DISPATCH,
            new io.casehub.engine.common.internal.event.JudgmentReDispatchEvent(
                event.caseId(), event.tenancyId(), event.bindingName(), ry.feedback(), null));
        judgmentNodeExecutor.enqueue(
            event.caseId(),
            event.bindingName(),
            new io.casehub.engine.common.spi.JudgmentNodeResult.ReYielded());
      }
      case io.casehub.api.spi.judgment.EscalationDecision.RouteHigher rh -> {
        LOG.infof(
            "Judgment route higher: caseId=%s binding=%s minimumTrust=%s",
            event.caseId(), event.bindingName(), rh.minimumTrustLevel());
        eventBus.publish(
            EventBusAddresses.JUDGMENT_RE_DISPATCH,
            new io.casehub.engine.common.internal.event.JudgmentReDispatchEvent(
                event.caseId(),
                event.tenancyId(),
                event.bindingName(),
                null,
                rh.minimumTrustLevel()));
        judgmentNodeExecutor.enqueue(
            event.caseId(),
            event.bindingName(),
            new io.casehub.engine.common.spi.JudgmentNodeResult.ReYielded());
      }
      case io.casehub.api.spi.judgment.EscalationDecision.Fault f -> {
        LOG.warnf(
            "Judgment faulted: caseId=%s binding=%s reason=%s",
            event.caseId(), event.bindingName(), f.reason());
        eventBus.publish(
            EventBusAddresses.JUDGMENT_FAULT,
            new io.casehub.engine.common.internal.event.JudgmentFaultEvent(
                event.caseId(), event.tenancyId(), event.bindingName(), f.reason()));
        judgmentNodeExecutor.enqueue(
            event.caseId(),
            event.bindingName(),
            new io.casehub.engine.common.spi.JudgmentNodeResult.Faulted(f.reason()));
      }
    }
  }

  private void writeEscalationEventLog(
      JudgmentEscalatedEvent event, io.casehub.api.spi.judgment.EscalationDecision decision) {
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
    String decisionType =
        switch (decision) {
          case io.casehub.api.spi.judgment.EscalationDecision.ReYield ry -> "re-yield";
          case io.casehub.api.spi.judgment.EscalationDecision.RouteHigher rh -> "route-higher";
          case io.casehub.api.spi.judgment.EscalationDecision.Fault f -> "fault";
        };
    metadata.put("decision", decisionType);
    log.setMetadata(metadata);
    eventLogRepository.append(log, event.tenancyId());
  }

  private int countPriorEscalations(java.util.UUID caseId, String bindingName, String tenancyId) {
    return (int)
        eventLogRepository
            .findByCaseAndTypes(
                caseId, java.util.List.of(CaseHubEventType.JUDGMENT_ESCALATED), tenancyId)
            .stream()
            .filter(
                e ->
                    e.getMetadata() != null
                        && bindingName.equals(e.getMetadata().path("bindingName").asText()))
            .count();
  }

  private io.casehub.api.model.CaseDefinition findDefinition(String bindingName) {
    return caseDefinitionRegistry.allDefinitions().stream()
        .filter(d -> d.getBindings().stream().anyMatch(b -> b.getName().equals(bindingName)))
        .findFirst()
        .orElse(null);
  }

  private io.casehub.api.model.Binding findBinding(String bindingName) {
    return caseDefinitionRegistry.allDefinitions().stream()
        .flatMap(d -> d.getBindings().stream())
        .filter(b -> b.getName().equals(bindingName))
        .findFirst()
        .orElse(null);
  }
}
