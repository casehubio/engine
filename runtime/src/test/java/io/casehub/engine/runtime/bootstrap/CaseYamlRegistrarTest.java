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
package io.casehub.engine.runtime.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseYamlRegistrarTest {

  @Mock CaseDefinitionRegistry caseDefinitionRegistry;
  @InjectMocks CaseYamlRegistrar registrar;

  @Test
  void sectionKey() {
    assertThat(registrar.sectionKey()).isEqualTo("cases");
  }

  @Test
  void classpathPaths() {
    assertThat(registrar.classpathPaths()).isEqualTo(Set.of("casehub", "cases"));
  }

  @Test
  void priority() {
    assertThat(registrar.priority()).isEqualTo(200);
  }

  @Test
  void register_validCaseYaml_registersDefinition() {
    String yaml =
        """
        dsl: "0.1.0"
        version: "1.0.0"
        name: test-case
        namespace: test
        spec:
          capabilities:
            - name: process
              inputProjection: "."
              outputProjection: "."
          bindings:
            - name: trigger
              capability: process
              on:
                contextChange:
                  filter: '.ready == true'
        """;

    registrar.register("casehub/test-case.yaml", stream(yaml));

    var captor = ArgumentCaptor.forClass(CaseDefinition.class);
    verify(caseDefinitionRegistry).registerCaseDefinition(captor.capture());

    CaseDefinition def = captor.getValue();
    assertThat(def.getName()).isEqualTo("test-case");
    assertThat(def.getNamespace()).isEqualTo("test");
    assertThat(def.getVersion()).isEqualTo("1.0.0");
  }

  @Test
  void register_invalidYaml_doesNotCrash() {
    String yaml = "not: a: valid: case: definition:\n  - broken";

    registrar.register("casehub/broken.yaml", stream(yaml));

    verify(caseDefinitionRegistry, never())
        .registerCaseDefinition(org.mockito.ArgumentMatchers.any());
  }

  private static ByteArrayInputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }
}
