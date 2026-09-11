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

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.ResolutionSelection;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.neocortex.memory.cbr.CbrFeedbackOutcome;
import io.casehub.neocortex.memory.cbr.CbrRetrievalFeedback;
import io.casehub.neocortex.memory.cbr.CbrRetrievalTracker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Layer 3 selection feedback — records retrieval relevance signals when a human or LLM selects a
 * resolution candidate via a judgment binding. Selected candidate receives HIGHLY_RELEVANT;
 * unselected candidates above a configurable similarity threshold receive PARTIALLY_RELEVANT.
 *
 * <p>Validates selectedCaseId against the presented _candidates list. Invalid selections are
 * rejected with a warning — no feedback is recorded for fabricated or stale IDs.
 *
 * <p>Transparent no-op when CbrRetrievalTracker is not on the classpath.
 *
 * <p>Refs casehubio/engine#1081.
 */
@ApplicationScoped
public class SelectionFeedbackRecorder {

  private static final Logger LOG = Logger.getLogger(SelectionFeedbackRecorder.class);

  @Inject Instance<CbrRetrievalTracker> cbrTracker;
  @Inject CaseDefinitionRegistry caseDefinitionRegistry;
  @Inject CaseInstanceRepository caseInstanceRepository;

  @ConfigProperty(name = "casehub.cbr.selection-feedback.threshold", defaultValue = "0.5")
  double similarityThreshold;

  void onPlanItemCompleted(@ObservesAsync PlanItemStateChangedEvent event) {
    if (event.newStatus() != TaskStatus.COMPLETED) return;
    if (!cbrTracker.isResolvable()) return;

    try {
      processCompletion(event.caseId(), event.bindingName(), event.tenancyId());
    } catch (Exception e) {
      LOG.warnf(
          "Selection feedback processing failed for caseId=%s binding=%s: %s",
          event.caseId(), event.bindingName(), e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  void processCompletion(java.util.UUID caseId, String bindingName, String tenancyId) {
    CaseInstance instance = caseInstanceRepository.findByUuid(caseId, tenancyId);
    if (instance == null) return;

    CaseDefinition definition =
        caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());
    if (definition == null) return;

    Binding binding =
        definition.getBindings().stream()
            .filter(b -> bindingName.equals(b.getName()))
            .findFirst()
            .orElse(null);
    if (binding == null) return;
    if (!(binding.target() instanceof JudgmentTarget jt)) return;
    if (jt.resolutionType() != ResolutionSelection.class) return;

    Map<String, Object> candidatesMap =
        (Map<String, Object>) instance.getCaseContext().get("_candidates");
    if (candidatesMap == null) return;

    List<Map<String, Object>> candidates =
        (List<Map<String, Object>>) candidatesMap.get(bindingName);
    if (candidates == null || candidates.isEmpty()) return;

    String outputKey =
        binding.getProducedKeys() != null && !binding.getProducedKeys().isEmpty()
            ? binding.getProducedKeys().iterator().next()
            : null;
    if (outputKey == null) return;

    Object rawSelection = instance.getCaseContext().get(outputKey);
    if (rawSelection == null) return;

    String selectedCaseId;
    if (rawSelection instanceof Map<?, ?> selMap) {
      selectedCaseId = (String) selMap.get("selectedCaseId");
    } else if (rawSelection instanceof ResolutionSelection sel) {
      selectedCaseId = sel.selectedCaseId();
    } else {
      return;
    }
    if (selectedCaseId == null) return;

    boolean validSelection =
        candidates.stream().anyMatch(c -> selectedCaseId.equals(c.get("caseId")));
    if (!validSelection) {
      LOG.warnf(
          "Invalid selection: selectedCaseId=%s not in _candidates.%s for caseId=%s — "
              + "skipping feedback to prevent feedback pollution",
          selectedCaseId, bindingName, caseId);
      return;
    }

    recordFeedback(caseId.toString(), tenancyId, selectedCaseId, candidates);
  }

  private void recordFeedback(
      String caseId,
      String tenancyId,
      String selectedCaseId,
      List<Map<String, Object>> candidates) {
    List<CbrRetrievalFeedback> entries = new ArrayList<>();

    for (Map<String, Object> candidate : candidates) {
      String candidateCaseId = (String) candidate.get("caseId");
      if (candidateCaseId == null) continue;

      if (candidateCaseId.equals(selectedCaseId)) {
        entries.add(new CbrRetrievalFeedback(candidateCaseId, CbrFeedbackOutcome.HIGHLY_RELEVANT));
      } else {
        Object simObj = candidate.get("similarity");
        double similarity = simObj instanceof Number n ? n.doubleValue() : 0.0;
        if (similarity >= similarityThreshold) {
          entries.add(
              new CbrRetrievalFeedback(candidateCaseId, CbrFeedbackOutcome.PARTIALLY_RELEVANT));
        }
      }
    }

    if (!entries.isEmpty()) {
      try {
        cbrTracker.get().feedback(caseId, tenancyId, entries);
        LOG.infof(
            "Selection feedback recorded: caseId=%s selected=%s candidates=%d",
            caseId, selectedCaseId, entries.size());
      } catch (Exception e) {
        LOG.warnf("Selection feedback recording failed for case %s: %s", caseId, e.getMessage());
      }
    }
  }
}
