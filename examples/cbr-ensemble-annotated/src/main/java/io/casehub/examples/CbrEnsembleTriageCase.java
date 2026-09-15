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
package io.casehub.examples;

import io.casehub.api.model.GoalExpression;
import io.casehub.engine.annotations.Bind;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Cbr;
import io.casehub.engine.annotations.Completion;
import io.casehub.engine.annotations.Feature;
import io.casehub.engine.annotations.Goal;
import io.casehub.engine.annotations.SystemPrompt;
import io.casehub.engine.annotations.Worker;
import java.util.List;

/**
 * CBR Ensemble Consensus — Incident Triage (Annotation pathway).
 *
 * <p>Same case as {@code examples/yaml/cbr-ensemble-consensus.yaml} and {@code
 * examples/cbr-ensemble-dsl/}. Demonstrates CBR with ensemble consensus — the triage agent reads
 * .cbrEnsemble.stepAnalysis from the case context to identify CONTESTED steps.
 *
 * <p>See also: examples/yaml/cbr-ensemble-consensus.yaml (YAML pathway) examples/cbr-ensemble-dsl/
 * (DSL pathway)
 */
@Case(
    namespace = "example",
    name = "EnsembleTriage",
    version = "1.0.0",
    title = "Ensemble Triage",
    summary = "Incident triage informed by CBR ensemble consensus")
@Cbr(
    features = {
      @Feature(name = "severity", expression = ".incident.severity"),
      @Feature(name = "category", expression = ".incident.category"),
      @Feature(name = "affectedSystem", expression = ".incident.affectedSystem")
    },
    domain = "incident-triage",
    caseType = "EnsembleTriage",
    topK = 5,
    minSimilarity = 0.3,
    problemDescription = ".incident.summary")
public interface CbrEnsembleTriageCase {

  @Worker(
      capability = "triage",
      description = "Triages incident using ensemble consensus from past responses")
  @Bind(contextChange = ".incident != null and .triageResult == null")
  @SystemPrompt(
      """
      You are an incident triage analyst. You receive an incident and
      CBR ensemble consensus from past similar incidents.

      Check .cbrEnsemble.scope — if "STEP_LEVEL", examine stepAnalysis:
      - UNANIMOUS steps: high confidence — follow these
      - CONSENSUS steps: reasonable confidence — follow with monitoring
      - CONTESTED steps: disagreement — flag for human review

      JQ to find contested steps:
        .cbrEnsemble.stepAnalysis[] | select(.agreement == "CONTESTED")

      Return severity, approach, contested steps, and confidence.""")
  TriageResult triage(Incident incident);

  @Worker(capability = "respond", description = "Executes the triage-recommended response")
  @Bind(contextChange = ".triageResult != null and .responseResult == null")
  @SystemPrompt("Execute the triage-recommended response actions for this incident.")
  ResponseResult respond(Incident incident, TriageResult triageResult);

  @Goal(value = "Incident resolved", condition = ".responseResult != null")
  @Completion
  default GoalExpression resolved() {
    return GoalExpression.goal("resolved");
  }

  record Incident(String severity, String category, String affectedSystem, String summary) {}

  record TriageResult(
      String severity, String approach, List<String> contestedSteps, double confidence) {}

  record ResponseResult(String status, List<String> actions) {}
}
