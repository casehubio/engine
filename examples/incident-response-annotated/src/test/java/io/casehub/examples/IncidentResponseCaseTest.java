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

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IncidentResponseCaseTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(
              root ->
                  root.addClasses(
                      IncidentResponseCase.class,
                      IncidentResponseCase.ScanResult.class,
                      IncidentResponseCase.TriageAssessment.class,
                      IncidentResponseCase.ContainmentResult.class,
                      IncidentResponseCase.RemediationReport.class,
                      IncidentResponseCase.ComplianceReport.class));

  @Inject CaseDefinition definition;

  @Test
  void namespace_and_name() {
    assertThat(definition.getNamespace()).isEqualTo("security");
    assertThat(definition.getName()).isEqualTo("IncidentResponse");
    assertThat(definition.getVersion()).isEqualTo("1.0.0");
  }

  @Test
  void five_workers() {
    assertThat(definition.getWorkers()).hasSize(5);
    assertThat(definition.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactlyInAnyOrder("scan", "triage", "contain", "remediate", "report");
  }

  @Test
  void five_capabilities() {
    assertThat(definition.getCapabilities()).hasSize(5);
    assertThat(definition.getCapabilities().stream().map(c -> c.name()).toList())
        .containsExactlyInAnyOrder(
            "scanForAnomalies",
            "triageSeverity",
            "containThreat",
            "remediate",
            "generateComplianceReport");
  }

  @Test
  void repeatable_bind_with_cron_on_scan() {
    long scanBindings =
        definition.getBindings().stream().filter(b -> b.getName().equals("scan")).count();
    assertThat(scanBindings).isEqualTo(2);
  }

  @Test
  void when_guard_on_triage() {
    var triageBinding =
        definition.getBindings().stream().filter(b -> b.getName().equals("triage")).findFirst();
    assertThat(triageBinding).isPresent();
    assertThat(triageBinding.get().getWhen()).isNotNull();
  }

  @Test
  void milestone_for_threat_containment() {
    assertThat(definition.getMilestones()).hasSize(1);
    assertThat(definition.getMilestones().get(0).getName()).isEqualTo("threatContained");
  }

  @Test
  void goal_for_resolution() {
    assertThat(definition.getGoals()).hasSize(1);
    assertThat(definition.getGoals().get(0).getName()).isEqualTo("resolved");
  }

  @Test
  void completion_wired() {
    assertThat(definition.getCompletion()).isNotNull();
  }

  @Test
  void summary_describes_purpose() {
    assertThat(definition.getSummary()).contains("cybersecurity");
  }
}
