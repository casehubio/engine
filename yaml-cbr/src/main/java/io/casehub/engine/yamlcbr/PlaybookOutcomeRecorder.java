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
package io.casehub.engine.yamlcbr;

import io.casehub.api.model.PlaybookExecutionResult;
import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrPlanStep;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.casehub.platform.api.path.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

public class PlaybookOutcomeRecorder {

  private static final Logger LOG = Logger.getLogger(PlaybookOutcomeRecorder.class);

  private final CbrRecordStore cbrStore;

  public PlaybookOutcomeRecorder(CbrRecordStore cbrStore) {
    this.cbrStore = cbrStore;
  }

  public void record(
      UUID caseId,
      String tenancyId,
      String caseType,
      String bindingName,
      PlaybookExecutionResult executionResult,
      int triageRound,
      List<Map<String, Object>> previousTriageModifications) {
    try {
      doRecord(
          caseId,
          tenancyId,
          caseType,
          bindingName,
          executionResult,
          triageRound,
          previousTriageModifications);
    } catch (Exception e) {
      LOG.warnf(
          e, "CBR playbook outcome recording failed for caseId=%s binding=%s", caseId, bindingName);
    }
  }

  private void doRecord(
      UUID caseId,
      String tenancyId,
      String caseType,
      String bindingName,
      PlaybookExecutionResult executionResult,
      int triageRound,
      List<Map<String, Object>> previousTriageModifications) {

    Map<String, FeatureValue> features =
        buildFeatures(executionResult, triageRound, previousTriageModifications);
    List<CbrPlanStep> steps = buildSteps(executionResult.steps());

    String outcome = resolveOutcome(executionResult);
    String problem = bindingName + " [playbook-execution]";
    String solution = outcome;

    var record = new CbrPlanRecord(problem, solution, outcome, null, features, steps, null, null);

    cbrStore.store(
        record,
        caseType,
        "playbook-" + bindingName + "-round-" + triageRound,
        new MemoryDomain(caseType),
        tenancyId,
        caseId.toString(),
        Path.root());
  }

  private Map<String, FeatureValue> buildFeatures(
      PlaybookExecutionResult result,
      int triageRound,
      List<Map<String, Object>> previousTriageModifications) {
    var features = new LinkedHashMap<String, FeatureValue>();

    features.put("outcome", FeatureValue.of(resolveOutcome(result)));
    features.put("stepCount", FeatureValue.of(result.steps().size()));
    features.put("triageRound", FeatureValue.of(triageRound));

    if (previousTriageModifications != null && !previousTriageModifications.isEmpty()) {
      features.put("triageModificationCount", FeatureValue.of(previousTriageModifications.size()));
    }

    long succeededCount =
        result.steps().stream().filter(s -> s.status() == StepStatus.SUCCEEDED).count();
    long failedCount = result.steps().stream().filter(s -> s.status() == StepStatus.FAILED).count();
    features.put("succeededSteps", FeatureValue.of(succeededCount));
    features.put("failedSteps", FeatureValue.of(failedCount));

    if (result instanceof PlaybookExecutionResult.Failure failure) {
      features.put("rootCause", FeatureValue.of(failure.rootCause()));
      result.steps().stream()
          .filter(s -> s.status() == StepStatus.FAILED)
          .findFirst()
          .ifPresent(s -> features.put("failedStepName", FeatureValue.of(s.stepName())));
    }

    return Map.copyOf(features);
  }

  private List<CbrPlanStep> buildSteps(List<StepResult> stepResults) {
    return stepResults.stream()
        .map(
            s ->
                new CbrPlanStep(
                    s.stepId(),
                    s.stepName(),
                    null,
                    s.status().name(),
                    0,
                    s.error() != null ? Map.of("error", (Object) s.error()) : Map.of(),
                    null))
        .toList();
  }

  private String resolveOutcome(PlaybookExecutionResult result) {
    return switch (result) {
      case PlaybookExecutionResult.Success ignored -> "SUCCESS";
      case PlaybookExecutionResult.PartialSuccess ignored -> "PARTIAL_SUCCESS";
      case PlaybookExecutionResult.Failure ignored -> "FAILURE";
    };
  }
}
