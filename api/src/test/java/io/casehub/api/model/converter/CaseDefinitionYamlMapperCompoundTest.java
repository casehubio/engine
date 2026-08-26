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
import io.casehub.api.model.CompoundDeclaration;
import io.casehub.api.model.Participation;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperCompoundTest {

  @Test
  void compounds_block_maps_to_compoundDeclarations() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          compounds:
            - name: risk-assessment-phase
              completionSemantics: all
              dispatchMode: ORCHESTRATED
              scopedBindings:
                collect-data: PARTICIPANT
                assess-risk: PARTICIPANT
            - name: approval-phase
              completionSemantics: firstWins
              dispatchMode: CHOREOGRAPHED
              scopedBindings:
                auto-approve: PARTICIPANT
                manual-approve: COMPANION
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    List<CompoundDeclaration> compounds = def.getCompounds();
    assertThat(compounds).hasSize(2);

    CompoundDeclaration phase1 = compounds.get(0);
    assertThat(phase1.name()).isEqualTo("risk-assessment-phase");
    assertThat(phase1.completionSemantics()).isEqualTo("all");
    assertThat(phase1.dispatchMode()).isEqualTo("ORCHESTRATED");
    assertThat(phase1.scopedBindings())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "collect-data", Participation.PARTICIPANT,
                "assess-risk", Participation.PARTICIPANT));
    assertThat(phase1.repeatable()).isFalse();
    assertThat(phase1.entryCondition()).isNull();
    assertThat(phase1.exitCondition()).isNull();
    assertThat(phase1.planningStrategy()).isNull();

    CompoundDeclaration phase2 = compounds.get(1);
    assertThat(phase2.name()).isEqualTo("approval-phase");
    assertThat(phase2.completionSemantics()).isEqualTo("firstWins");
    assertThat(phase2.dispatchMode()).isEqualTo("CHOREOGRAPHED");
    assertThat(phase2.scopedBindings())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "auto-approve", Participation.PARTICIPANT,
                "manual-approve", Participation.COMPANION));
  }

  @Test
  void compound_with_m_of_n_completion() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          compounds:
            - name: quorum-phase
              completionSemantics: "3"
              scopedBindings:
                voter-a: PARTICIPANT
                voter-b: PARTICIPANT
                voter-c: PARTICIPANT
                voter-d: PARTICIPANT
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    CompoundDeclaration compound = def.getCompounds().get(0);
    assertThat(compound.name()).isEqualTo("quorum-phase");
    assertThat(compound.completionSemantics()).isEqualTo("3");
    assertThat(compound.dispatchMode()).isEqualTo("CHOREOGRAPHED");
    assertThat(compound.scopedBindings()).hasSize(4);
  }

  @Test
  void compound_with_conditions_and_repeatable() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          compounds:
            - name: retry-phase
              completionSemantics: all
              dispatchMode: ORCHESTRATED
              repeatable: true
              entryCondition: '.status == "active"'
              exitCondition: '.retryCount >= 3'
              planningStrategy: goap
              scopedBindings:
                retry-action: PARTICIPANT
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    CompoundDeclaration compound = def.getCompounds().get(0);
    assertThat(compound.name()).isEqualTo("retry-phase");
    assertThat(compound.repeatable()).isTrue();
    assertThat(compound.entryCondition()).isNotNull();
    assertThat(compound.exitCondition()).isNotNull();
    assertThat(compound.planningStrategy()).isEqualTo("goap");
  }

  @Test
  void compound_with_defaults() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          compounds:
            - name: simple-phase
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    CompoundDeclaration compound = def.getCompounds().get(0);
    assertThat(compound.name()).isEqualTo("simple-phase");
    assertThat(compound.completionSemantics()).isEqualTo("all");
    assertThat(compound.dispatchMode()).isEqualTo("CHOREOGRAPHED");
    assertThat(compound.scopedBindings()).isEmpty();
    assertThat(compound.repeatable()).isFalse();
  }

  @Test
  void no_compounds_block_returns_empty() throws IOException {
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

    assertThat(def.getCompounds()).isEmpty();
  }
}
