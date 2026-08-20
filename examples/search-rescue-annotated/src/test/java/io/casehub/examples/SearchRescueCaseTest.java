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

class SearchRescueCaseTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(
              root ->
                  root.addClasses(
                      SearchRescueCase.class,
                      SearchRescueCase.ConditionAssessment.class,
                      SearchRescueCase.DroneSearchResult.class,
                      SearchRescueCase.MedicalAssessment.class,
                      SearchRescueCase.RescueResult.class,
                      SearchRescueCase.EvacuationPlan.class));

  @Inject CaseDefinition definition;

  @Test
  void goap_planning_strategy() {
    assertThat(definition.getPlanningStrategy()).isEqualTo("goap");
  }

  @Test
  void namespace_and_name() {
    assertThat(definition.getNamespace()).isEqualTo("emergency");
    assertThat(definition.getName()).isEqualTo("SearchAndRescue");
  }

  @Test
  void four_workers_plus_standalone_capability() {
    assertThat(definition.getWorkers()).hasSize(4);
    assertThat(definition.getCapabilities()).hasSize(5);
    assertThat(definition.getCapabilities().stream().map(c -> c.name()).toList())
        .contains("fieldRescueTeam");
  }

  @Test
  void goap_dependency_chain() {
    var assessAction =
        definition.getGoapActions().stream()
            .filter(a -> a.name().equals("assessConditions"))
            .findFirst();
    assertThat(assessAction).isPresent();
    assertThat(assessAction.get().preconditions()).isEmpty();

    var droneAction =
        definition.getGoapActions().stream()
            .filter(a -> a.name().equals("deployDrones"))
            .findFirst();
    assertThat(droneAction).isPresent();
    assertThat(droneAction.get().preconditions()).containsKey("conditionAssessment");
  }

  @Test
  void goal_and_completion() {
    assertThat(definition.getGoals()).hasSize(1);
    assertThat(definition.getGoals().get(0).getName()).isEqualTo("rescued");
    assertThat(definition.getCompletion()).isNotNull();
  }

  @Test
  void summary() {
    assertThat(definition.getSummary()).contains("search and rescue");
  }
}
