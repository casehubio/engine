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
package io.casehub.blackboard.subcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.DefaultSubCaseCompletionStrategy;
import io.casehub.api.model.SubCaseCompletionStrategy;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.CaseLifecycleEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.work.CaseResumptionService;
import io.casehub.engine.spi.EventLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Listens for terminal {@link CaseLifecycleEvent} CDI events. When the terminating case is a child
 * case (its UUID appears in a parent's SUBCASE_STARTED EventLog entry), updates the parent context
 * and resumes the parent if it was WAITING.
 *
 * <p>See casehubio/engine#195.
 */
@ApplicationScoped
public class SubCaseCompletionListener {

  private static final Logger LOG = Logger.getLogger(SubCaseCompletionListener.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject EventLogRepository eventLogRepository;
  @Inject CaseInstanceCache caseInstanceCache;
  @Inject CaseResumptionService caseResumptionService;

  public void onCaseLifecycle(@ObservesAsync CaseLifecycleEvent event) {
    if (!isTerminal(event.commandType())) return;

    UUID childCaseId = event.caseId();

    // Find parent SUBCASE_STARTED entry for this child
    List<EventLog> subcaseStartedList =
        eventLogRepository
            .findByTypes(List.of(CaseHubEventType.SUBCASE_STARTED))
            .await()
            .atMost(Duration.ofSeconds(10));

    EventLog startedEntry =
        subcaseStartedList.stream()
            .filter(
                e -> {
                  JsonNode meta = e.getMetadata();
                  return meta != null
                      && childCaseId
                          .toString()
                          .equals(
                              meta.has("childCaseId") ? meta.get("childCaseId").asText() : null);
                })
            .findFirst()
            .orElse(null);

    if (startedEntry == null) return;

    UUID parentCaseId = startedEntry.getCaseId();
    String outputMapping =
        startedEntry.getMetadata().has("outputMapping")
            ? startedEntry.getMetadata().get("outputMapping").asText()
            : null;

    CaseInstance parent = caseInstanceCache.get(parentCaseId);
    if (parent == null) {
      LOG.warnf("SubCaseCompletionListener: parent case %s not in cache", parentCaseId);
      return;
    }

    // Load child's terminal status
    CaseStatus childStatus =
        event.caseStatus() != null ? CaseStatus.valueOf(event.caseStatus()) : CaseStatus.FAULTED;

    // Apply outputMapping: evaluate against child's final context, merge result into parent
    if (outputMapping != null) {
      CaseInstance child = caseInstanceCache.get(childCaseId);
      if (child != null) {
        Map<String, Object> mapped = child.getCaseContext().evalObjectTemplate(outputMapping);
        if (mapped != null) {
          mapped.forEach((k, v) -> parent.getCaseContext().set(k, v));
        }
      } else {
        LOG.warnf(
            "SubCaseCompletionListener: child %s not in cache — outputMapping skipped",
            childCaseId);
      }
    }

    // Check completion strategy
    SubCaseCompletionStrategy strategy = new DefaultSubCaseCompletionStrategy();
    SubCaseCompletionStrategy.ItemStatus itemStatus = strategy.mapToStageItemStatus(childStatus);
    LOG.infof(
        "SubCaseCompletionListener: child %s (%s) → parent %s itemStatus=%s",
        childCaseId, childStatus, parentCaseId, itemStatus);

    // Write SUBCASE_COMPLETED EventLog on parent
    EventLog completedLog = new EventLog();
    completedLog.setCaseId(parentCaseId);
    completedLog.setWorkerId(childCaseId.toString());
    completedLog.setEventType(CaseHubEventType.SUBCASE_COMPLETED);
    completedLog.setStreamType(EventStreamType.CASE);
    completedLog.setTimestamp(Instant.now());
    ObjectNode meta = OBJECT_MAPPER.createObjectNode();
    meta.put("childCaseId", childCaseId.toString());
    meta.put("childFinalStatus", childStatus.name());
    completedLog.setMetadata(meta);

    eventLogRepository.append(completedLog).await().atMost(Duration.ofSeconds(10));

    // Resume parent
    Map<String, Object> childOutput = Map.of();
    caseResumptionService
        .resumeIfWaiting(
            parent,
            childCaseId.toString(),
            childCaseId.toString(),
            childOutput,
            CaseHubEventType.SUBCASE_COMPLETED)
        .await()
        .atMost(Duration.ofSeconds(10));
  }

  private static boolean isTerminal(String commandType) {
    return "CompleteCase".equals(commandType)
        || "FaultCase".equals(commandType)
        || "CancelCase".equals(commandType);
  }
}
