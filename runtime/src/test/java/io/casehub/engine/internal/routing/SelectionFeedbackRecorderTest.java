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

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.ResolutionSelection;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.internal.context.CaseContextImpl;
import io.casehub.neocortex.memory.cbr.CbrFeedbackOutcome;
import io.casehub.neocortex.memory.cbr.CbrRetrievalFeedback;
import io.casehub.neocortex.memory.cbr.CbrRetrievalTracker;
import jakarta.enterprise.inject.Instance;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for SelectionFeedbackRecorder — verifies selection validation and Layer 3 feedback
 * mapping. Refs casehubio/engine#1081.
 */
class SelectionFeedbackRecorderTest {

  private static final UUID CASE_ID = UUID.randomUUID();
  private static final String TENANCY_ID = "test-tenant";
  private static final String BINDING_NAME = "select-resolution";

  private SelectionFeedbackRecorder recorder;
  private CbrRetrievalTracker tracker;
  private CaseDefinitionRegistry registry;
  private CaseInstanceRepository caseInstanceRepo;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    tracker = mock(CbrRetrievalTracker.class);
    registry = mock(CaseDefinitionRegistry.class);
    caseInstanceRepo = mock(CaseInstanceRepository.class);

    Instance<CbrRetrievalTracker> trackerInstance = mock(Instance.class);
    when(trackerInstance.isResolvable()).thenReturn(true);
    when(trackerInstance.get()).thenReturn(tracker);

    recorder = new SelectionFeedbackRecorder();
    setField(recorder, "cbrTracker", trackerInstance);
    setField(recorder, "caseDefinitionRegistry", registry);
    setField(recorder, "caseInstanceRepository", caseInstanceRepo);
    setField(recorder, "similarityThreshold", 0.5);
  }

  @Test
  void selectedCandidateRecordsHighlyRelevant() {
    setupCaseWithCandidatesAndSelection(
        List.of(candidate("case-1", 0.9), candidate("case-2", 0.7), candidate("case-3", 0.3)),
        Map.of(
            "selectedCaseId", "case-1",
            "sourceType", "PLAN_TRACE"));

    recorder.processCompletion(CASE_ID, BINDING_NAME, TENANCY_ID);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CbrRetrievalFeedback>> captor = ArgumentCaptor.forClass(List.class);
    verify(tracker).feedback(eq(CASE_ID.toString()), eq(TENANCY_ID), captor.capture());

    List<CbrRetrievalFeedback> entries = captor.getValue();
    assertThat(entries).hasSize(2);
    assertThat(
            entries.stream()
                .filter(e -> e.outcome() == CbrFeedbackOutcome.HIGHLY_RELEVANT)
                .toList())
        .hasSize(1)
        .allMatch(e -> "case-1".equals(e.tracedCaseId()));
    assertThat(
            entries.stream()
                .filter(e -> e.outcome() == CbrFeedbackOutcome.PARTIALLY_RELEVANT)
                .toList())
        .hasSize(1)
        .allMatch(e -> "case-2".equals(e.tracedCaseId()));
  }

  @Test
  void unselectedBelowThresholdGetsNoSignal() {
    setupCaseWithCandidatesAndSelection(
        List.of(candidate("case-1", 0.9), candidate("case-3", 0.3)),
        Map.of(
            "selectedCaseId", "case-1",
            "sourceType", "PLAN_TRACE"));

    recorder.processCompletion(CASE_ID, BINDING_NAME, TENANCY_ID);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CbrRetrievalFeedback>> captor = ArgumentCaptor.forClass(List.class);
    verify(tracker).feedback(any(), any(), captor.capture());

    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0).tracedCaseId()).isEqualTo("case-1");
  }

  @Test
  void invalidSelectionRejectsWithNoFeedback() {
    setupCaseWithCandidatesAndSelection(
        List.of(candidate("case-1", 0.9), candidate("case-2", 0.7)),
        Map.of(
            "selectedCaseId", "fabricated-id",
            "sourceType", "PLAN_TRACE"));

    recorder.processCompletion(CASE_ID, BINDING_NAME, TENANCY_ID);

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @Test
  void noOpWhenBindingIsNotJudgmentTarget() {
    CaseDefinition def =
        CaseDefinition.builder()
            .namespace("test")
            .name("non-judgment")
            .version("1.0.0")
            .binding(
                Binding.builder()
                    .name(BINDING_NAME)
                    .capability(new io.casehub.worker.api.Capability("cap", ".", ".", ""))
                    .on("true")
                    .build())
            .build();

    CaseMetaModel metaModel = new CaseMetaModel();
    metaModel.setName("non-judgment");
    metaModel.setNamespace("test");
    metaModel.setVersion("1.0.0");

    CaseInstance instance = new CaseInstance();
    instance.setUuid(CASE_ID);
    instance.setCaseMetaModel(metaModel);
    instance.setCaseContext(new CaseContextImpl(Map.of()));
    instance.tenancyId = TENANCY_ID;

    when(caseInstanceRepo.findByUuid(CASE_ID, TENANCY_ID)).thenReturn(instance);
    when(registry.getCaseDefinition(metaModel)).thenReturn(def);

    recorder.processCompletion(CASE_ID, BINDING_NAME, TENANCY_ID);

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @Test
  void noOpWhenNoCandidatesInContext() {
    setupCaseWithCandidatesAndSelection(List.of(), Map.of("selectedCaseId", "case-1"));

    recorder.processCompletion(CASE_ID, BINDING_NAME, TENANCY_ID);

    verify(tracker, never()).feedback(any(), any(), any());
  }

  @Test
  void trackerExceptionDoesNotPropagate() {
    setupCaseWithCandidatesAndSelection(
        List.of(candidate("case-1", 0.9)),
        Map.of("selectedCaseId", "case-1", "sourceType", "PLAN_TRACE"));

    org.mockito.Mockito.doThrow(new RuntimeException("storage unavailable"))
        .when(tracker)
        .feedback(any(), any(), any());

    // Should not throw
    recorder.processCompletion(CASE_ID, BINDING_NAME, TENANCY_ID);
  }

  private void setupCaseWithCandidatesAndSelection(
      List<Map<String, Object>> candidates, Map<String, Object> selection) {
    var contextData = new java.util.LinkedHashMap<String, Object>();
    if (!candidates.isEmpty()) {
      contextData.put("_candidates", Map.of(BINDING_NAME, candidates));
    }
    contextData.put("selectedResolution", selection);

    CaseDefinition def =
        CaseDefinition.builder()
            .namespace("test")
            .name("judgment-test")
            .version("1.0.0")
            .binding(
                Binding.builder()
                    .name(BINDING_NAME)
                    .judgment(
                        JudgmentTarget.builder()
                            .prompt("Select approach")
                            .resolutionType(ResolutionSelection.class)
                            .build())
                    .on("true")
                    .producedKeys(java.util.Set.of("selectedResolution"))
                    .build())
            .build();

    CaseMetaModel metaModel = new CaseMetaModel();
    metaModel.setName("judgment-test");
    metaModel.setNamespace("test");
    metaModel.setVersion("1.0.0");

    CaseInstance instance = new CaseInstance();
    instance.setUuid(CASE_ID);
    instance.setCaseMetaModel(metaModel);
    instance.setCaseContext(new CaseContextImpl(contextData));
    instance.tenancyId = TENANCY_ID;

    when(caseInstanceRepo.findByUuid(CASE_ID, TENANCY_ID)).thenReturn(instance);
    when(registry.getCaseDefinition(metaModel)).thenReturn(def);
  }

  private static Map<String, Object> candidate(String caseId, double similarity) {
    var map = new java.util.LinkedHashMap<String, Object>();
    map.put("caseId", caseId);
    map.put("sourceType", "PLAN_TRACE");
    map.put("similarity", similarity);
    map.put("caseType", "test-type");
    map.put("problem", "test problem");
    map.put("confidence", 0.9);
    map.put("stepCount", 3);
    return map;
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
