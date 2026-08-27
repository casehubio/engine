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
package io.casehub.api.model.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.plan.goap.GoapAction;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperGoapTest {

  @Test
  void actions_block_maps_to_goapActions() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          actions:
            - name: assess-risk
              preconditions:
                dataCollected: true
              effects:
                riskAssessed: true
              cost: 2.0
            - name: collect-data
              preconditions:
                caseOpened: true
              effects:
                dataCollected: true
              cost: 1.5
              benefit: 0.3
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    List<GoapAction> actions = def.getGoapActions();
    assertThat(actions).hasSize(2);

    GoapAction assessRisk = actions.get(0);
    assertThat(assessRisk.name()).isEqualTo("assess-risk");
    assertThat(assessRisk.preconditions()).containsExactlyEntriesOf(Map.of("dataCollected", true));
    assertThat(assessRisk.effects()).containsExactlyEntriesOf(Map.of("riskAssessed", true));
    assertThat(assessRisk.cost()).isEqualTo(2.0);
    assertThat(assessRisk.benefit()).isEqualTo(0.0);
    assertThat(assessRisk.softPreconditions()).isEmpty();
    assertThat(assessRisk.costFunction()).isNull();

    GoapAction collectData = actions.get(1);
    assertThat(collectData.name()).isEqualTo("collect-data");
    assertThat(collectData.preconditions()).containsExactlyEntriesOf(Map.of("caseOpened", true));
    assertThat(collectData.effects()).containsExactlyEntriesOf(Map.of("dataCollected", true));
    assertThat(collectData.cost()).isEqualTo(1.5);
    assertThat(collectData.benefit()).isEqualTo(0.3);
  }

  @Test
  void actions_with_soft_preconditions() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          actions:
            - name: escalate
              preconditions:
                riskAssessed: true
              effects:
                escalated: true
              cost: 3.0
              benefit: 0.5
              softPreconditions:
                managerAvailable: true
                priorApproval: false
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    List<GoapAction> actions = def.getGoapActions();
    assertThat(actions).hasSize(1);

    GoapAction escalate = actions.get(0);
    assertThat(escalate.name()).isEqualTo("escalate");
    assertThat(escalate.softPreconditions())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("managerAvailable", true, "priorApproval", false));
    assertThat(escalate.benefit()).isEqualTo(0.5);
  }

  @Test
  void actions_with_defaults() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          actions:
            - name: simple-action
              preconditions:
                ready: true
              effects:
                done: true
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    List<GoapAction> actions = def.getGoapActions();
    assertThat(actions).hasSize(1);

    GoapAction action = actions.get(0);
    assertThat(action.name()).isEqualTo("simple-action");
    assertThat(action.cost()).isEqualTo(1.0);
    assertThat(action.benefit()).isEqualTo(0.0);
    assertThat(action.softPreconditions()).isEmpty();
  }

  @Test
  void no_actions_block_leaves_goapActions_null() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          planningStrategy: goap
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    assertThat(def.getGoapActions()).isNullOrEmpty();
  }

  @Test
  void actions_with_multiple_preconditions_and_effects() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          actions:
            - name: full-review
              preconditions:
                dataCollected: true
                riskAssessed: true
                notEscalated: false
              effects:
                reviewed: true
                approved: true
              cost: 5.0
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    GoapAction action = def.getGoapActions().get(0);
    assertThat(action.preconditions())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of("dataCollected", true, "riskAssessed", true, "notEscalated", false));
    assertThat(action.effects())
        .containsExactlyInAnyOrderEntriesOf(Map.of("reviewed", true, "approved", true));
    assertThat(action.cost()).isEqualTo(5.0);
  }
}
