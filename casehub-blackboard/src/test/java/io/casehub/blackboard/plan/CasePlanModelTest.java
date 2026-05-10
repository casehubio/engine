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
package io.casehub.blackboard.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.CaseDefinition;
import io.casehub.blackboard.stage.Stage;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CasePlanModelTest {

  private static CaseDefinition def(String name) {
    return CaseDefinition.builder().namespace("test").name(name).version("1.0").build();
  }

  private static Stage stage(String name) {
    return Stage.builder().name(name).entry(".x == true").build();
  }

  @Test
  void nameRequired() {
    assertThatThrownBy(() -> CasePlanModel.builder().build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void stagesAreRetained() {
    Stage s1 = stage("s1");
    Stage s2 = stage("s2");
    CasePlanModel model = CasePlanModel.builder().name("m").stages(s1, s2).build();
    assertThat(model.getStages()).containsExactly(s1, s2);
  }

  @Test
  void stagesDefaultToEmpty() {
    CasePlanModel model = CasePlanModel.builder().name("m").build();
    assertThat(model.getStages()).isEmpty();
  }

  @Test
  void registryReturnsEmptyForUnknownDefinition() {
    CasePlanModelRegistry registry = new CasePlanModelRegistry();
    Optional<CasePlanModel> result = registry.find(def("unknown"));
    assertThat(result).isEmpty();
  }

  @Test
  void registryReturnsPlanForRegisteredDefinition() {
    CasePlanModelRegistry registry = new CasePlanModelRegistry();
    CaseDefinition definition = def("my-case");
    CasePlanModel model = CasePlanModel.builder().name("m").stages(stage("s1")).build();
    registry.register(definition, model);
    assertThat(registry.find(definition)).contains(model);
  }

  @Test
  void registryMatchesByNamespaceNameVersion() {
    CasePlanModelRegistry registry = new CasePlanModelRegistry();
    CaseDefinition def1 = def("case-a");
    CaseDefinition def2 = def("case-b");
    CasePlanModel model1 = CasePlanModel.builder().name("m1").build();
    CasePlanModel model2 = CasePlanModel.builder().name("m2").build();
    registry.register(def1, model1);
    registry.register(def2, model2);
    assertThat(registry.find(def1)).contains(model1);
    assertThat(registry.find(def2)).contains(model2);
  }
}
