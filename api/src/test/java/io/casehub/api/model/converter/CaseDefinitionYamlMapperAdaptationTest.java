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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.CaseDefinition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperAdaptationTest {

  private CaseDefinition load(String yaml) throws IOException {
    InputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    return CaseDefinitionYamlMapper.load(in);
  }

  @Test
  void parsesExplicitAdaptationConfig() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          adaptation:
            trigger: every-step
            revision: forward-replan
        """;
    CaseDefinition def = load(yaml);
    var config = def.getAdaptationConfig();
    assertThat(config).isNotNull();
    assertThat(config.trigger()).isEqualTo("every-step");
    assertThat(config.revision()).isEqualTo("forward-replan");
  }

  @Test
  void parsesAdaptivePreset() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          adaptation: adaptive
        """;
    CaseDefinition def = load(yaml);
    var config = def.getAdaptationConfig();
    assertThat(config).isNotNull();
    assertThat(config.trigger()).isEqualTo("every-step");
    assertThat(config.revision()).isEqualTo("forward-replan");
  }

  @Test
  void parsesConservativePreset() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          adaptation: conservative
        """;
    CaseDefinition def = load(yaml);
    var config = def.getAdaptationConfig();
    assertThat(config).isNotNull();
    assertThat(config.trigger()).isEqualTo("on-failure");
    assertThat(config.revision()).isEqualTo("forward-replan");
  }

  @Test
  void missingAdaptationReturnsNull() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        """;
    CaseDefinition def = load(yaml);
    assertThat(def.getAdaptationConfig()).isNull();
  }

  @Test
  void partialExplicitConfigUsesDefaults() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          adaptation:
            trigger: on-failure
        """;
    CaseDefinition def = load(yaml);
    var config = def.getAdaptationConfig();
    assertThat(config).isNotNull();
    assertThat(config.trigger()).isEqualTo("on-failure");
    assertThat(config.revision()).isEqualTo("forward-replan");
  }

  @Test
  void unknownPresetThrows() {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          adaptation: unknown-preset
        """;
    assertThatThrownBy(() -> load(yaml)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void offPresetReturnsNull() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          adaptation: "off"
        """;
    CaseDefinition def = load(yaml);
    assertThat(def.getAdaptationConfig()).isNull();
  }

  @Test
  void parsesProgressPreset() throws IOException {
    String yaml =
        """
                dsl: "0.1.0"
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  adaptation: progress
                """;
    CaseDefinition def = load(yaml);
    var config = def.getAdaptationConfig();
    assertThat(config).isNotNull();
    assertThat(config.trigger()).isEqualTo("progress");
    assertThat(config.revision()).isEqualTo("forward-replan");
    assertThat(config.threshold()).isEqualTo(0.3);
  }

  @Test
  void parsesProgressWithCustomThreshold() throws IOException {
    String yaml =
        """
                dsl: "0.1.0"
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  adaptation:
                    trigger: progress
                    threshold: 0.4
                """;
    CaseDefinition def = load(yaml);
    var config = def.getAdaptationConfig();
    assertThat(config).isNotNull();
    assertThat(config.trigger()).isEqualTo("progress");
    assertThat(config.threshold()).isEqualTo(0.4);
  }

  @Test
  void explicitConfigWithoutThresholdHasNullThreshold() throws IOException {
    String yaml =
        """
                dsl: "0.1.0"
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  adaptation:
                    trigger: every-step
                    revision: forward-replan
                """;
    CaseDefinition def = load(yaml);
    assertThat(def.getAdaptationConfig().threshold()).isNull();
  }

  @Test
  void parsesReplanAfterOnBinding() throws IOException {
    String yaml =
        """
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  capabilities:
                    - name: analyse
                  workers:
                    - name: analyst
                      capabilities: [analyse]
                  bindings:
                    - name: analyse-binding
                      capability: analyse
                      on:
                        contextChange:
                          filter: ".ready"
                      replanAfter: always
                """;
    CaseDefinition def = load(yaml);
    var binding = def.getBindings().get(0);
    assertThat(binding.getReplanHint()).isEqualTo(io.casehub.api.model.ReplanHint.ALWAYS);
  }

  @Test
  void replanAfterDefaultsToConditional() throws IOException {
    String yaml =
        """
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  capabilities:
                    - name: analyse
                  workers:
                    - name: analyst
                      capabilities: [analyse]
                  bindings:
                    - name: analyse-binding
                      capability: analyse
                      on:
                        contextChange:
                          filter: ".ready"
                """;
    CaseDefinition def = load(yaml);
    var binding = def.getBindings().get(0);
    assertThat(binding.getReplanHint()).isEqualTo(io.casehub.api.model.ReplanHint.CONDITIONAL);
  }

  @Test
  void parsesReplanAfterNever() throws IOException {
    String yaml =
        """
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  capabilities:
                    - name: analyse
                  workers:
                    - name: analyst
                      capabilities: [analyse]
                  bindings:
                    - name: analyse-binding
                      capability: analyse
                      on:
                        contextChange:
                          filter: ".ready"
                      replanAfter: never
                """;
    CaseDefinition def = load(yaml);
    var binding = def.getBindings().get(0);
    assertThat(binding.getReplanHint()).isEqualTo(io.casehub.api.model.ReplanHint.NEVER);
  }

  @Test
  void parsesPortfolioConfig() throws IOException {
    String yaml =
        """
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  decompositionStrategy: portfolio
                  portfolioConfig:
                    delegates: [goap, llm]
                    timeouts:
                      goap: 1000
                      llm: 30000
                """;
    CaseDefinition def = load(yaml);
    assertThat(def.getDecompositionStrategy()).isEqualTo("portfolio");
    assertThat(def.getPortfolioConfig()).isNotNull();
    assertThat(def.getPortfolioConfig().delegates()).containsExactly("goap", "llm");
    assertThat(def.getPortfolioConfig().timeoutFor("goap")).isEqualTo(1000L);
  }

  @Test
  void portfolioWithoutConfigHasNullPortfolioConfig() throws IOException {
    String yaml =
        """
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  decompositionStrategy: portfolio
                """;
    CaseDefinition def = load(yaml);
    assertThat(def.getDecompositionStrategy()).isEqualTo("portfolio");
    assertThat(def.getPortfolioConfig()).isNull();
  }

  @Test
  void portfolioConfigWithOnlyDelegates() throws IOException {
    String yaml =
        """
                namespace: test
                name: test-case
                version: "1.0.0"
                spec:
                  decompositionStrategy: portfolio
                  portfolioConfig:
                    delegates: [llm]
                """;
    CaseDefinition def = load(yaml);
    assertThat(def.getPortfolioConfig()).isNotNull();
    assertThat(def.getPortfolioConfig().delegates()).containsExactly("llm");
  }
}
