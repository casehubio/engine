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
package io.casehub.engine.internal.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.spi.StepOutcomeEvent;
import io.casehub.api.spi.StepOutcomeObserver;
import io.casehub.api.spi.routing.RetrievedExperience;
import io.casehub.api.spi.routing.RoutingOutcome;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.neocortex.memory.cbr.CbrFeedbackOutcome;
import io.casehub.neocortex.memory.cbr.CbrRetrievalFeedback;
import io.casehub.neocortex.memory.cbr.CbrRetrievalTracker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Layer 1 retrieval feedback — correlates CBR retrieval traces with worker outcomes via the
 * StepOutcomeObserver SPI. On each worker completion, reads the experiences that were retrieved at
 * dispatch time from EventLog metadata, maps the worker outcome to a feedback signal, and records
 * it via CbrRetrievalTracker.
 *
 * <p>Transparent no-op when CbrRetrievalTracker is not on the classpath.
 *
 * <p>Refs casehubio/engine#1081.
 */
@ApplicationScoped
public class RetrievalFeedbackObserver implements StepOutcomeObserver {

  private static final Logger LOG = Logger.getLogger(RetrievalFeedbackObserver.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject Instance<CbrRetrievalTracker> cbrTracker;

  @Inject EventLogRepository eventLogRepository;

  @Override
  public void onStepOutcome(StepOutcomeEvent event) {
    if (!cbrTracker.isResolvable()) {
      return;
    }

    List<RetrievedExperience> experiences =
        loadExperiencesFromEventLog(event.caseId(), event.workerName(), event.tenancyId());
    if (experiences.isEmpty()) {
      return;
    }

    List<CbrRetrievalFeedback> feedbackEntries =
        mapOutcomeToFeedback(event.outcome(), event.workerName(), experiences);
    if (feedbackEntries.isEmpty()) {
      return;
    }

    try {
      cbrTracker.get().feedback(event.caseId().toString(), event.tenancyId(), feedbackEntries);
      LOG.infof(
          "Retrieval feedback recorded: caseId=%s outcome=%s experiences=%d",
          event.caseId(), event.outcome(), feedbackEntries.size());
    } catch (Exception e) {
      LOG.warnf(
          "Retrieval feedback recording failed for case %s: %s", event.caseId(), e.getMessage());
    }
  }

  private List<RetrievedExperience> loadExperiencesFromEventLog(
      java.util.UUID caseId, String workerName, String tenancyId) {
    try {
      List<EventLog> logs =
          eventLogRepository.findByCaseAndWorkerAndType(
              caseId, workerName, CaseHubEventType.WORKER_SCHEDULED, tenancyId);
      if (logs.isEmpty()) {
        return List.of();
      }
      EventLog mostRecent = logs.get(logs.size() - 1);
      return deserializeExperiences(mostRecent);
    } catch (Exception e) {
      LOG.warnf(
          "Failed to load experiences from EventLog for case %s worker %s: %s",
          caseId, workerName, e.getMessage());
      return List.of();
    }
  }

  private static List<RetrievedExperience> deserializeExperiences(EventLog eventLog) {
    JsonNode experiencesNode = eventLog.getMetadata().get("experiences");
    if (experiencesNode == null || experiencesNode.isNull() || experiencesNode.isEmpty()) {
      return List.of();
    }
    try {
      return OBJECT_MAPPER.convertValue(
          experiencesNode,
          OBJECT_MAPPER
              .getTypeFactory()
              .constructCollectionType(List.class, RetrievedExperience.class));
    } catch (Exception e) {
      LOG.warnf(e, "Failed to deserialize CBR experiences from EventLog — skipping feedback");
      return List.of();
    }
  }

  private static List<CbrRetrievalFeedback> mapOutcomeToFeedback(
      RoutingOutcome outcome, String workerName, List<RetrievedExperience> experiences) {
    return switch (outcome) {
      case SUCCESS ->
          experiences.stream()
              .filter(e -> e.caseId() != null)
              .map(e -> new CbrRetrievalFeedback(e.caseId(), CbrFeedbackOutcome.RELEVANT))
              .toList();
      case DECLINED -> {
        List<CbrRetrievalFeedback> entries = new ArrayList<>();
        for (RetrievedExperience exp : experiences) {
          if (exp.caseId() == null) continue;
          boolean isDeclinedAgent =
              exp.planTrace() != null
                  && exp.planTrace().stream()
                      .anyMatch(step -> workerName.equals(step.workerName()));
          if (isDeclinedAgent) {
            entries.add(new CbrRetrievalFeedback(exp.caseId(), CbrFeedbackOutcome.NOT_RELEVANT));
          }
        }
        yield entries;
      }
      case FAILURE ->
          experiences.stream()
              .filter(e -> e.caseId() != null)
              .map(e -> new CbrRetrievalFeedback(e.caseId(), CbrFeedbackOutcome.NOT_RELEVANT))
              .toList();
      case GATE_REJECTED, GATE_EXPIRED, CANCELLED, OBSOLETE -> List.of();
    };
  }
}
