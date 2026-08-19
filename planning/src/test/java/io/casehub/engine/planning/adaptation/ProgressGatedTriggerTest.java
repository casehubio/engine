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
package io.casehub.engine.planning.adaptation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.casehub.api.model.AdaptationConfig;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.ReplanHint;
import io.casehub.api.model.TaskStatus;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.event.EventStreamType;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.plan.adaptation.AdaptationContext;
import io.casehub.engine.plan.adaptation.AdaptationSignal;
import io.casehub.engine.plan.monitoring.MonitoringConfig;
import io.casehub.worker.api.Capability;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProgressGatedTriggerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final String TENANT = "tenant-1";
  private static final String COMPOUND_ID = "goal-compound";
  private static final String BINDING_NAME = "cap-a";

  private StubEventLogRepository eventLogRepository;
  private ProgressGatedTrigger trigger;

  @BeforeEach
  void setUp() {
    eventLogRepository = new StubEventLogRepository();
    trigger = new ProgressGatedTrigger(eventLogRepository);
  }

  @Test
  void idIsProgress() {
    assertEquals("progress", trigger.id());
  }

  @Test
  void replanHintAlwaysProceedsRegardlessOfDivergence() {
    var def =
        buildDefinition(
            ReplanHint.ALWAYS,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void replanHintNeverSkipsEvenOnFailure() {
    var def =
        buildDefinition(
            ReplanHint.NEVER,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.FAULTED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void failureStatusAlwaysProceeds() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.FAULTED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void failureStatusProceedsOnRejected() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.REJECTED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void failureStatusProceedsOnCancelled() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.CANCELLED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void noMonitoringConfigSkipsOnSuccess() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL, null, new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void monitoringDisabledSkipsOnSuccess() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.disabled(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void noMonitoringConfigStillProceedsOnFailure() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL, null, new AdaptationConfig("progress", "forward-replan", 0.3));
    var ctx = buildContext(def, TaskStatus.FAULTED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void belowThresholdSkips() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.5));
    eventLogRepository.setCompletions(List.of(completionEntry(COMPOUND_ID, 0.2, 0)));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void aboveThresholdProceeds() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    eventLogRepository.setCompletions(List.of(completionEntry(COMPOUND_ID, 0.5, 0)));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void exactlyAtThresholdSkips() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.5));
    eventLogRepository.setCompletions(List.of(completionEntry(COMPOUND_ID, 0.5, 0)));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void noExpectationDataSkipsOnSuccess() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    eventLogRepository.setCompletions(List.of());
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void windowedAverageAboveThresholdProceeds() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            new MonitoringConfig(true, 0.5, 3),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    eventLogRepository.setCompletions(
        List.of(
            completionEntry(COMPOUND_ID, 0.2, 0),
            completionEntry(COMPOUND_ID, 0.5, 0),
            completionEntry(COMPOUND_ID, 0.4, 0)));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  @Test
  void filtersOutDifferentCompound() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    eventLogRepository.setCompletions(
        List.of(completionEntry("other-compound", 0.9, 0), completionEntry(COMPOUND_ID, 0.1, 0)));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void filtersOutDifferentGeneration() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            new AdaptationConfig("progress", "forward-replan", 0.3));
    eventLogRepository.setCompletions(
        List.of(completionEntry(COMPOUND_ID, 0.9, 0), completionEntry(COMPOUND_ID, 0.1, 1)));
    var ctx = buildContextWithGeneration(def, TaskStatus.COMPLETED, 1);
    assertEquals(AdaptationSignal.SKIP, trigger.evaluate(ctx));
  }

  @Test
  void defaultThresholdUsedWhenConfigThresholdNull() {
    var def =
        buildDefinition(
            ReplanHint.CONDITIONAL,
            MonitoringConfig.defaults(),
            AdaptationConfig.of("progress", "forward-replan"));
    eventLogRepository.setCompletions(List.of(completionEntry(COMPOUND_ID, 0.35, 0)));
    var ctx = buildContext(def, TaskStatus.COMPLETED);
    assertEquals(AdaptationSignal.PROCEED, trigger.evaluate(ctx));
  }

  // --- helpers ---

  private CaseDefinition buildDefinition(
      ReplanHint hint, MonitoringConfig monitoring, AdaptationConfig adaptation) {
    var def =
        CaseDefinition.builder()
            .namespace("test")
            .name("test-case")
            .version("1.0")
            .adaptationConfig(adaptation)
            .bindings(
                Binding.builder()
                    .name(BINDING_NAME)
                    .capability(Capability.of("cap-a", ".", "."))
                    .on(new ContextChangeTrigger(".ready == true"))
                    .replanHint(hint)
                    .build())
            .build();
    if (monitoring != null) {
      def.setMonitoringConfig(monitoring);
    }
    return def;
  }

  private AdaptationContext buildContext(CaseDefinition def, TaskStatus status) {
    return buildContextWithGeneration(def, status, 0);
  }

  private AdaptationContext buildContextWithGeneration(
      CaseDefinition def, TaskStatus status, int generation) {
    return new AdaptationContext(
        CASE_ID,
        TENANT,
        COMPOUND_ID,
        COMPOUND_ID,
        List.of(),
        List.of(),
        List.of(),
        JsonNodeFactory.instance.objectNode(),
        def,
        status,
        BINDING_NAME,
        generation);
  }

  private EventLog completionEntry(String compoundId, double divergenceRatio, int generation) {
    var entry = new EventLog();
    entry.setCaseId(CASE_ID);
    entry.setEventType(CaseHubEventType.WORKER_EXECUTION_COMPLETED);
    entry.setStreamType(EventStreamType.CASE);
    entry.setTimestamp(Instant.now());

    var meta = MAPPER.createObjectNode();
    var validation = MAPPER.createObjectNode();
    validation.put("compoundId", compoundId);
    validation.put("divergenceRatio", divergenceRatio);
    validation.put("adaptationGeneration", generation);
    validation.put("totalExpectedEffects", 3);
    validation.put("violatedEffectCount", (int) (divergenceRatio * 3));
    meta.set("expectationValidation", validation);
    entry.setMetadata(meta);
    return entry;
  }

  static class StubEventLogRepository implements EventLogRepository {

    private List<EventLog> completions = new ArrayList<>();

    void setCompletions(List<EventLog> completions) {
      this.completions = new ArrayList<>(completions);
    }

    @Override
    public void append(EventLog eventLog, String tenancyId) {}

    @Override
    public Long appendAndReturnId(EventLog eventLog, String tenancyId) {
      return 1L;
    }

    @Override
    public EventLog findById(Long id, String tenancyId) {
      return null;
    }

    @Override
    public List<EventLog> findSchedulingEvents(
        UUID caseId, String workerId, Instant after, String tenancyId) {
      return List.of();
    }

    @Override
    public List<EventLog> findByCaseAndTypes(
        UUID caseId, Collection<CaseHubEventType> types, String tenancyId) {
      return completions;
    }

    @Override
    public List<EventLog> findByCaseAndWorkerAndType(
        UUID caseId, String workerId, CaseHubEventType type, String tenancyId) {
      return List.of();
    }

    @Override
    public List<EventLog> findByWorkerAndType(
        String workerId, CaseHubEventType type, String tenancyId) {
      return List.of();
    }

    @Override
    public List<EventLog> findByCaseWithFilters(
        UUID caseId,
        Collection<CaseHubEventType> eventTypes,
        Collection<EventStreamType> streamTypes,
        String tenancyId) {
      return List.of();
    }
  }
}
