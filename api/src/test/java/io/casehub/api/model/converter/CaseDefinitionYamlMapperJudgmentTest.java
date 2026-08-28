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

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.JudgmentTarget;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperJudgmentTest {

  private CaseDefinition loadDefinition(String filename) {
    InputStream is = getClass().getClassLoader().getResourceAsStream("yaml/" + filename);
    try {
      return CaseDefinitionYamlMapper.load(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void judgmentBinding_fullFields_parsedFromYaml() {
    CaseDefinition def = loadDefinition("judgment-test.yaml");
    Binding binding =
        def.getBindings().stream()
            .filter(b -> b.getName().equals("risk-judgment"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.target()).isInstanceOf(JudgmentTarget.class);
    JudgmentTarget target = (JudgmentTarget) binding.target();
    assertThat(target.prompt()).isEqualTo("Assess the risk");
    assertThat(target.inputMapping()).isNotNull();
    assertThat(target.outputMapping()).isNotNull();
    assertThat(target.expiresIn()).isEqualTo(Duration.ofHours(1));
    assertThat(target.evidenceRequirements()).containsExactly("riskScore", "rationale");
  }

  @Test
  void judgmentBinding_dynamicPrompt_parsedFromYaml() {
    CaseDefinition def = loadDefinition("judgment-test.yaml");
    Binding binding =
        def.getBindings().stream()
            .filter(b -> b.getName().equals("dynamic-judgment"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.target()).isInstanceOf(JudgmentTarget.class);
    JudgmentTarget target = (JudgmentTarget) binding.target();
    assertThat(target.prompt()).isNull();
    assertThat(target.promptExpression()).isNotNull();
  }

  @Test
  void judgmentBinding_minimalPromptOnly() {
    CaseDefinition def = loadDefinition("judgment-test.yaml");
    Binding binding =
        def.getBindings().stream()
            .filter(b -> b.getName().equals("simple-judgment"))
            .findFirst()
            .orElseThrow();
    JudgmentTarget target = (JudgmentTarget) binding.target();
    assertThat(target.prompt()).isEqualTo("Is this correct?");
    assertThat(target.evidenceRequirements()).isEmpty();
    assertThat(target.expiresIn()).isNull();
  }
}
