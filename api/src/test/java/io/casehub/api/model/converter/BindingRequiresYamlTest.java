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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.yaml.jackson.YamlMappers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BindingRequiresYamlTest {

  private CaseDefinition load(String yaml) throws IOException {
    return CaseDefinitionYamlMapper.load(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)),
        YamlMappers.create(),
        null,
        null,
        null,
        null);
  }

  @Test
  void parsesRequiresFromYaml() throws IOException {
    String yaml =
        """
        dsl: 0.1.0
        namespace: test
        name: quorum-test
        version: 1.0.0
        spec:
          capabilities:
            - name: extract
            - name: aggregate
          workers:
            - name: extractor
              capabilities: [extract]
            - name: aggregator
              capabilities: [aggregate]
          bindings:
            - name: extract-a
              capability: extract
              on: { contextChange: { filter: ".go" } }
            - name: extract-b
              capability: extract
              on: { contextChange: { filter: ".go" } }
            - name: aggregate
              capability: aggregate
              on: { contextChange: { filter: ".go" } }
              requires:
                bindings: [extract-a, extract-b]
                atLeast: 1
                onThresholdReached: KEEP
        """;
    CaseDefinition def = load(yaml);
    Binding agg =
        def.getBindings().stream()
            .filter(b -> "aggregate".equals(b.getName()))
            .findFirst()
            .orElseThrow();
    assertNotNull(agg.getRequires());
    assertEquals(2, agg.getRequires().bindings().size());
    assertEquals(1, agg.getRequires().atLeast());
    assertEquals(OnThresholdReached.KEEP, agg.getRequires().onThresholdReached());
  }

  @Test
  void bindingWithoutRequiresHasNullRequires() throws IOException {
    String yaml =
        """
        dsl: 0.1.0
        namespace: test
        name: no-requires
        version: 1.0.0
        spec:
          capabilities:
            - name: extract
          workers:
            - name: extractor
              capabilities: [extract]
          bindings:
            - name: extract-a
              capability: extract
              on: { contextChange: { filter: ".go" } }
        """;
    CaseDefinition def = load(yaml);
    assertNull(def.getBindings().get(0).getRequires());
  }
}
