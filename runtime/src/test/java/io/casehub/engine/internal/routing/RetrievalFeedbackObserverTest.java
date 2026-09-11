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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.spi.StepOutcomeEvent;
import io.casehub.api.spi.routing.ExperiencePlanStep;
import io.casehub.api.spi.routing.RetrievedExperience;
import io.casehub.api.spi.routing.RoutingOutcome;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.neocortex.memory.cbr.CbrFeedbackOutcome;
import io.casehub.neocortex.memory.cbr.CbrRetrievalFeedback;
import io.casehub.neocortex.memory.cbr.CbrRetrievalTracker;
import jakarta.enterprise.inject.Instance;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for RetrievalFeedbackObserver — verifies outcome-to-feedback mapping and EventLog
 * correlation. Refs casehubio/engine#1081.
 */
class RetrievalFeedbackObserverTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final String TENANCY_ID = "test-tenant";
  private static final String WORKER_NAME = "investigation-agent";
  private static final String BINDING_NAME = "investigate";

  private RetrievalFeedbackObserver observer;
  private CbrRetrievalTracker tracker;
  private EventLogRepository eventLogRepo;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    tracker = mock(CbrRetrievalTracker.class);
    eventLogRepo = mock(EventLogRepository.class);

    Instance<CbrRetrievalTracker> trackerInstance = mock(Instance.class);
    when(trackerInstance.isResolvable()).thenReturn(true);
    when(trackerInstance.get()).thenReturn(tracker);

    observer = new RetrievalFeedbackObserver();
    setField(observer, "cbrTracker", trackerInstance);
    setField(observer, "eventLogRepository", eventLogRepo);
  }

  @Test
  void successOutcomeRecordsRelevantFeedback() {
    setupEventLogWithExperiences(
        List.of(buildExperience("cbr-case-1", 0.8), buildExperience("cbr-case-2", 0.6)));

    observer.onStepOutcome(buildEvent(RoutingOutcome.SUCCESS));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CbrRetrievalFeedback>> captor = ArgumentCaptor.forClass(List.class);
    verify(tracker).feedback(eq(CASE_ID.toString()), eq(TENANCY_ID), captor.capture());

    List<CbrRetrievalFeedback> entries = captor.getValue();
    assertThat(entries).hasSize(2);
    assertThat(entries).allMatch(e -> e.outcome() == CbrFeedbackOutcome.RELEVANT);
    assertThat(entries.stream().map(CbrRetrievalFeedback::tracedCaseId).toList())
        .containsExactly("cbr-case-1", "cbr-case-2");
  }

  @Test
  void declinedOutcomeRecordsNotRelevantOnlyForDeclinedAgentExperiences() {
    var expWithAgent =
        buildExperienceWithPlanTrace(
            "cbr-case-1",
            0.8,
            List.of(
                new ExperiencePlanStep(
                    BINDING_NAME, "investigate", WORKER_NAME, RoutingOutcome.SUCCESS, 1, null)));
    var expWithoutAgent =
        buildExperienceWithPlanTrace(
            "cbr-case-2",
            0.6,
            List.of(
                new ExperiencePlanStep(
                    "other-binding", "other-cap", "other-agent", RoutingOutcome.SUCCESS, 1, null)));

    setupEventLogWithExperiences(List.of(expWithAgent, expWithoutAgent));

    observer.onStepOutcome(buildEvent(RoutingOutcome.DECLINED));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CbrRetrievalFeedback>> captor = ArgumentCaptor.forClass(List.class);
    verify(tracker).feedback(eq(CASE_ID.toString()), eq(TENANCY_ID), captor.capture());

    List<CbrRetrievalFeedback> entries = captor.getValue();
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).tracedCaseId()).isEqualTo("cbr-case-1");
    assertThat(entries.get(0).outcome()).isEqualTo(CbrFeedbackOutcome.NOT_RELEVANT);
  }

  @Test
  void failureOutcomeRecordsNotRelevantForAllExperiences() {
    setupEventLogWithExperiences(
        List.of(buildExperience("cbr-case-1", 0.8), buildExperience("cbr-case-2", 0.6)));

    observer.onStepOutcome(buildEvent(RoutingOutcome.FAILURE));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CbrRetrievalFeedback>> captor = ArgumentCaptor.forClass(List.class);
    verify(tracker).feedback(eq(CASE_ID.toString()), eq(TENANCY_ID), captor.capture());

    List<CbrRetrievalFeedback> entries = captor.getValue();
    assertThat(entries).hasSize(2);
    assertThat(entries).allMatch(e -> e.outcome() == CbrFeedbackOutcome.NOT_RELEVANT);
  }

  @Test
  void gateRejectedGeneratesNoFeedback() {
    setupEventLogWithExperiences(List.of(buildExperience("cbr-case-1", 0.8)));

    observer.onStepOutcome(buildEvent(RoutingOutcome.GATE_REJECTED));

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @Test
  void cancelledGeneratesNoFeedback() {
    setupEventLogWithExperiences(List.of(buildExperience("cbr-case-1", 0.8)));

    observer.onStepOutcome(buildEvent(RoutingOutcome.CANCELLED));

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @Test
  void noOpWhenNoExperiencesInEventLog() {
    when(eventLogRepo.findByCaseAndWorkerAndType(
            CASE_ID, WORKER_NAME, CaseHubEventType.WORKER_SCHEDULED, TENANCY_ID))
        .thenReturn(List.of());

    observer.onStepOutcome(buildEvent(RoutingOutcome.SUCCESS));

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @SuppressWarnings("unchecked")
  @Test
  void noOpWhenTrackerNotResolvable() {
    Instance<CbrRetrievalTracker> absentTracker = mock(Instance.class);
    when(absentTracker.isResolvable()).thenReturn(false);
    setField(observer, "cbrTracker", absentTracker);

    observer.onStepOutcome(buildEvent(RoutingOutcome.SUCCESS));

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @Test
  void trackerExceptionDoesNotPropagate() {
    setupEventLogWithExperiences(List.of(buildExperience("cbr-case-1", 0.8)));
    when(tracker.toString()).thenReturn("mock-tracker");
    org.mockito.Mockito.doThrow(new RuntimeException("storage unavailable"))
        .when(tracker)
        .feedback(any(), any(), any());

    // Should not throw
    observer.onStepOutcome(buildEvent(RoutingOutcome.SUCCESS));
  }

  @Test
  void experiencesWithNullCaseIdAreFiltered() {
    var expWithId = buildExperience("cbr-case-1", 0.8);
    var expWithoutId =
        new RetrievedExperience(
            "problem", "solution", "COMPLETED", 0.9, 0.7, Map.of(), List.of(), Map.of());

    setupEventLogWithExperiences(List.of(expWithId, expWithoutId));

    observer.onStepOutcome(buildEvent(RoutingOutcome.SUCCESS));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CbrRetrievalFeedback>> captor = ArgumentCaptor.forClass(List.class);
    verify(tracker).feedback(eq(CASE_ID.toString()), eq(TENANCY_ID), captor.capture());

    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).tracedCaseId()).isEqualTo("cbr-case-1");
  }

  private StepOutcomeEvent buildEvent(RoutingOutcome outcome) {
    return new StepOutcomeEvent(
        CASE_ID,
        TENANCY_ID,
        "test-case-type",
        BINDING_NAME,
        "investigate",
        WORKER_NAME,
        outcome,
        Map.of("alert", "phishing"),
        Duration.ofSeconds(5));
  }

  private RetrievedExperience buildExperience(String caseId, double similarity) {
    return new RetrievedExperience(
        "problem",
        "solution",
        "COMPLETED",
        (Double) 0.9,
        similarity,
        Map.<String, Object>of(),
        List.<ExperiencePlanStep>of(),
        Map.<String, Double>of(),
        "test-type",
        io.casehub.api.spi.routing.ResolutionSourceType.PLAN_TRACE,
        null,
        null,
        caseId);
  }

  private RetrievedExperience buildExperienceWithPlanTrace(
      String caseId, double similarity, List<ExperiencePlanStep> trace) {
    return new RetrievedExperience(
        "problem",
        "solution",
        "COMPLETED",
        (Double) 0.9,
        similarity,
        Map.<String, Object>of(),
        trace,
        Map.<String, Double>of(),
        "test-type",
        io.casehub.api.spi.routing.ResolutionSourceType.PLAN_TRACE,
        null,
        null,
        caseId);
  }

  private void setupEventLogWithExperiences(List<RetrievedExperience> experiences) {
    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.set("experiences", MAPPER.valueToTree(experiences));
    metadata.put("bindingName", BINDING_NAME);

    EventLog eventLog = new EventLog();
    eventLog.setMetadata(metadata);

    when(eventLogRepo.findByCaseAndWorkerAndType(
            CASE_ID, WORKER_NAME, CaseHubEventType.WORKER_SCHEDULED, TENANCY_ID))
        .thenReturn(List.of(eventLog));
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      var field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
