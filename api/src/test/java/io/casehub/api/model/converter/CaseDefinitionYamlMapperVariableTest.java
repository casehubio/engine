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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.api.model.CaseDefinition;
import io.casehub.yaml.core.resolver.VariableSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperVariableTest {

  private CaseDefinition loadWithVariables(String yaml, Map<String, VariableSource> sources)
      throws IOException {
    return CaseDefinitionYamlMapper.load(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
        new ObjectMapper(new YAMLFactory()),
        null,
        null,
        sources);
  }

  @Test
  void resolvesEnvironmentVariables() throws Exception {
    var yaml =
        """
        name: ${env.APP_NAME}
        namespace: io.casehub.test
        version: "1.0"
        spec:
          capabilities:
            - name: analysis
        workers:
          - name: worker
            capabilities: [analysis]
        bindings:
          - name: trigger
            capability: analysis
            on:
              contextChange: {}
        """;
    var sources =
        Map.of(
            "env",
            (VariableSource)
                name ->
                    switch (name) {
                      case "APP_NAME" -> "resolved-app";
                      default -> null;
                    });
    var def = loadWithVariables(yaml, sources);
    assertThat(def.getName()).isEqualTo("resolved-app");
  }

  @Test
  void variablesResolvedBeforeForEach() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: ["${env.REGION_A}", "${env.REGION_B}"]
            as: region
        spec:
          capabilities:
            - name: process
        workers:
          - name: processor-${each.region}
            forEach: regions
            capabilities: [process]
        bindings:
          - name: trigger
            capability: process
            on:
              contextChange: {}
        """;
    var sources =
        Map.of(
            "env",
            (VariableSource)
                name ->
                    switch (name) {
                      case "REGION_A" -> "eu-west";
                      case "REGION_B" -> "us-east";
                      default -> null;
                    });
    var def = loadWithVariables(yaml, sources);
    assertThat(def.getWorkers()).hasSize(2);
    assertThat(def.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactly("processor-eu-west", "processor-us-east");
  }

  @Test
  void noVariableSources_passesThrough() throws Exception {
    var yaml =
        """
        name: test-app
        namespace: io.casehub.test
        version: "1.0"
        spec:
          capabilities:
            - name: analysis
        workers:
          - name: worker
            capabilities: [analysis]
        bindings:
          - name: trigger
            capability: analysis
            on:
              contextChange: {}
        """;
    var def = loadWithVariables(yaml, Map.of());
    assertThat(def.getName()).isEqualTo("test-app");
  }

  @Test
  void configVariables() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "${config.VERSION}"
        spec:
          capabilities:
            - name: analysis
        workers:
          - name: worker
            capabilities: [analysis]
        bindings:
          - name: trigger
            capability: analysis
            on:
              contextChange: {}
        """;
    var sources =
        Map.of(
            "config",
            (VariableSource)
                name ->
                    switch (name) {
                      case "VERSION" -> "2.5.0";
                      default -> null;
                    });
    var def = loadWithVariables(yaml, sources);
    assertThat(def.getVersion()).isEqualTo("2.5.0");
  }

  @Test
  void defaultValues() throws Exception {
    var yaml =
        """
        name: ${env.APP_NAME:-fallback-app}
        namespace: io.casehub.test
        version: "1.0"
        spec:
          capabilities:
            - name: analysis
        workers:
          - name: worker
            capabilities: [analysis]
        bindings:
          - name: trigger
            capability: analysis
            on:
              contextChange: {}
        """;
    var sources = Map.of("env", (VariableSource) name -> null);
    var def = loadWithVariables(yaml, sources);
    assertThat(def.getName()).isEqualTo("fallback-app");
  }
}
