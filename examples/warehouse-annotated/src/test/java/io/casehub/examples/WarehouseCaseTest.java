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

class WarehouseCaseTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(
              root ->
                  root.addClasses(
                      WarehouseCase.class,
                      WarehouseCase.PickRoute.class,
                      WarehouseCase.PickResult.class,
                      WarehouseCase.QualityReport.class,
                      WarehouseCase.HazmatClearance.class,
                      WarehouseCase.DispatchConfirmation.class));

  @Inject CaseDefinition definition;

  @Test
  void goap_planning() {
    assertThat(definition.getPlanningStrategy()).isEqualTo("goap");
  }

  @Test
  void namespace_and_name() {
    assertThat(definition.getNamespace()).isEqualTo("logistics");
    assertThat(definition.getName()).isEqualTo("WarehouseFulfillment");
  }

  @Test
  void five_workers() {
    assertThat(definition.getWorkers()).hasSize(5);
    assertThat(definition.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactlyInAnyOrder(
            "planRoute", "pickItems", "qualityCheck", "handleHazmat", "packAndDispatch");
  }

  @Test
  void soft_dependency_on_hazmat() {
    var packAction =
        definition.getGoapActions().stream()
            .filter(a -> a.name().equals("packAndDispatch"))
            .findFirst();
    assertThat(packAction).isPresent();
    assertThat(packAction.get().softPreconditions()).containsKey("hazmatClearance");
    assertThat(packAction.get().preconditions()).doesNotContainKey("hazmatClearance");
  }

  @Test
  void goap_chain_planRoute_has_no_domain_preconditions() {
    var routeAction =
        definition.getGoapActions().stream().filter(a -> a.name().equals("planRoute")).findFirst();
    assertThat(routeAction).isPresent();
    assertThat(routeAction.get().preconditions()).isEmpty();
  }

  @Test
  void goal_and_completion() {
    assertThat(definition.getGoals()).hasSize(1);
    assertThat(definition.getGoals().get(0).getName()).isEqualTo("dispatched");
    assertThat(definition.getCompletion()).isNotNull();
  }
}
