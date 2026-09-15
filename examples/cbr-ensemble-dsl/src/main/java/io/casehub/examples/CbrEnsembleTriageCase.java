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

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.StandardGoalKind;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.platform.api.path.Path;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;

/**
 * CBR Ensemble Consensus — Incident Triage (DSL pathway).
 *
 * <p>Same case as {@code examples/yaml/cbr-ensemble-consensus.yaml} and {@code
 * examples/cbr-ensemble-annotated/}. Demonstrates CBR with ensemble consensus — the triage agent
 * reads .cbrEnsemble.stepAnalysis from the case context to identify CONTESTED steps.
 *
 * <p>See also: examples/yaml/cbr-ensemble-consensus.yaml (YAML pathway)
 * examples/cbr-ensemble-annotated/ (annotation pathway)
 */
public final class CbrEnsembleTriageCase {

  private CbrEnsembleTriageCase() {}

  public static CaseDefinition define() {
    Capability triage =
        Capability.of(
            "triage",
            "{ incident: .incident, ensemble: .cbrEnsemble }",
            "{ triageResult: { severity: .severity, approach: .approach,"
                + " contestedSteps: .contestedSteps, confidence: .confidence } }");

    Capability respond =
        Capability.of(
            "respond",
            "{ incident: .incident, triageResult: .triageResult }",
            "{ responseResult: { status: .status, actions: .actions } }");

    CbrConfig cbrConfig =
        CbrConfig.builder()
            .feature("severity", ".incident.severity")
            .feature("category", ".incident.category")
            .feature("affectedSystem", ".incident.affectedSystem")
            .domain("incident-triage")
            .caseType("ensemble-triage")
            .topK(5)
            .minSimilarity(0.3)
            .problemDescription(".incident.summary")
            .build();

    return CaseDefinition.builder()
        .namespace("example")
        .name("ensemble-triage")
        .version("1.0.0")
        .title("Ensemble Triage")
        .summary(
            "Incident triage informed by CBR ensemble consensus"
                + " — identifies agreed and contested response steps")
        .type(Path.parse("example/cbr"))
        .label(Path.parse("example/ensemble"))
        .cbrConfig(cbrConfig)
        .capabilities(triage, respond)
        .workers(
            Worker.builder().name("triage-analyst").capabilityName("triage").noFunction().build(),
            Worker.builder().name("responder").capabilityName("respond").noFunction().build())
        .bindings(
            Binding.builder()
                .name("triage-on-incident")
                .capability(triage)
                .on(new ContextChangeTrigger(".incident != null and .triageResult == null"))
                .build(),
            Binding.builder()
                .name("respond-after-triage")
                .capability(respond)
                .on(new ContextChangeTrigger(".triageResult != null and .responseResult == null"))
                .build())
        .goals(
            Goal.builder()
                .name("incidentResolved")
                .kind(StandardGoalKind.SUCCESS)
                .condition(".responseResult != null")
                .build())
        .completion(GoalExpression.goal("incidentResolved"))
        .build();
  }
}
