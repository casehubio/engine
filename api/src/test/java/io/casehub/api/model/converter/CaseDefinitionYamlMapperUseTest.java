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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperUseTest {

  @Test
  void load_useBlock_secretsAndConfigMaps() throws IOException {
    String yaml =
        """
        namespace: test
        name: use-test
        version: 1.0.0
        dsl: 0.1.0
        use:
          secrets:
            - openai
            - anthropic
          configMaps:
            - app-config
            - model-params
        spec:
          capabilities: []
          workers: []
          bindings: []
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(def.getUse()).isNotNull();
    assertThat(def.getUse().getSecrets()).containsExactlyInAnyOrder("openai", "anthropic");
    assertThat(def.getUse().getConfigMaps())
        .containsExactlyInAnyOrder("app-config", "model-params");
  }

  @Test
  void load_useBlock_secretsOnly() throws IOException {
    String yaml =
        """
        namespace: test
        name: secrets-only
        version: 1.0.0
        dsl: 0.1.0
        use:
          secrets:
            - database
        spec:
          capabilities: []
          workers: []
          bindings: []
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(def.getUse()).isNotNull();
    assertThat(def.getUse().getSecrets()).containsExactly("database");
    assertThat(def.getUse().getConfigMaps()).isEmpty();
  }

  @Test
  void load_useBlock_configMapsOnly() throws IOException {
    String yaml =
        """
        namespace: test
        name: configmaps-only
        version: 1.0.0
        dsl: 0.1.0
        use:
          configMaps:
            - feature-flags
        spec:
          capabilities: []
          workers: []
          bindings: []
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(def.getUse()).isNotNull();
    assertThat(def.getUse().getSecrets()).isEmpty();
    assertThat(def.getUse().getConfigMaps()).containsExactly("feature-flags");
  }

  @Test
  void load_noUseBlock_useIsNull() throws IOException {
    String yaml =
        """
        namespace: test
        name: no-use
        version: 1.0.0
        dsl: 0.1.0
        spec:
          capabilities: []
          workers: []
          bindings: []
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(def.getUse()).isNull();
  }
}
