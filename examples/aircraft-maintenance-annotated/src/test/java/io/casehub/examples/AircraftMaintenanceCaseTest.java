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

class AircraftMaintenanceCaseTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(
              root ->
                  root.addClasses(
                      AircraftMaintenanceCase.class,
                      AircraftMaintenanceCase.DefectAssessment.class,
                      AircraftMaintenanceCase.RepairPlan.class,
                      AircraftMaintenanceCase.PartsApproval.class,
                      AircraftMaintenanceCase.RepairRecord.class,
                      AircraftMaintenanceCase.AirworthinessCertification.class));

  @Inject CaseDefinition definition;

  @Test
  void namespace_and_name() {
    assertThat(definition.getNamespace()).isEqualTo("aviation");
    assertThat(definition.getName()).isEqualTo("AircraftMaintenance");
    assertThat(definition.getVersion()).isEqualTo("1.0.0");
  }

  @Test
  void five_workers() {
    assertThat(definition.getWorkers()).hasSize(5);
    assertThat(definition.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactlyInAnyOrder(
            "assessDefect", "planRepair", "approveParts", "executeRepair", "certify");
  }

  @Test
  void five_capabilities() {
    assertThat(definition.getCapabilities()).hasSize(5);
  }

  @Test
  void milestone_for_repair_completion() {
    assertThat(definition.getMilestones()).hasSize(1);
    assertThat(definition.getMilestones().get(0).getName()).isEqualTo("repairComplete");
  }

  @Test
  void goal_for_airworthiness() {
    assertThat(definition.getGoals()).hasSize(1);
    assertThat(definition.getGoals().get(0).getName()).isEqualTo("certified");
  }

  @Test
  void completion_wired() {
    assertThat(definition.getCompletion()).isNotNull();
  }

  @Test
  void summary_describes_purpose() {
    assertThat(definition.getSummary()).contains("airworthiness");
  }
}
