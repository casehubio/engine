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
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperForEachTest {

  private CaseDefinition load(String yaml) throws IOException {
    return CaseDefinitionYamlMapper.load(
        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void expandsWorkerForEach() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: [eu, us, ap]
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
    var def = load(yaml);
    assertThat(def.getWorkers()).hasSize(3);
    assertThat(def.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactly("processor-eu", "processor-us", "processor-ap");
  }

  @Test
  void expandsWorkerForEachUnderSpec() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: [eu, us]
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
    var def = load(yaml);
    assertThat(def.getWorkers()).hasSize(2);
    assertThat(def.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactly("processor-eu", "processor-us");
  }

  @Test
  void expandsBindingForEach() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: [eu, us]
            as: region
        spec:
          capabilities:
            - name: process
        workers:
          - name: processor
            capabilities: [process]
        bindings:
          - name: trigger-${each.region}
            forEach: regions
            capability: process
            on:
              contextChange: {}
        """;
    var def = load(yaml);
    assertThat(def.getBindings()).hasSize(2);
    assertThat(def.getBindings().stream().map(b -> b.getName()).toList())
        .containsExactly("trigger-eu", "trigger-us");
  }

  @Test
  void noForEach_passesThrough() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        spec:
          capabilities:
            - name: process
        workers:
          - name: processor
            capabilities: [process]
        bindings:
          - name: trigger
            capability: process
            on:
              contextChange: {}
        """;
    var def = load(yaml);
    assertThat(def.getWorkers()).hasSize(1);
    assertThat(def.getWorkers().get(0).name()).isEqualTo("processor");
  }

  @Test
  void mixedForEachAndStatic() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: [eu, us]
            as: region
        spec:
          capabilities:
            - name: process
            - name: monitor
        workers:
          - name: processor-${each.region}
            forEach: regions
            capabilities: [process]
          - name: global-monitor
            capabilities: [monitor]
        bindings:
          - name: trigger
            capability: process
            on:
              contextChange: {}
        """;
    var def = load(yaml);
    assertThat(def.getWorkers()).hasSize(3);
    assertThat(def.getWorkers().stream().map(w -> w.name()).toList())
        .containsExactly("processor-eu", "processor-us", "global-monitor");
  }

  @Test
  void unknownIterationGroup_throws() {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: [eu, us]
            as: region
        spec:
          capabilities:
            - name: process
        workers:
          - name: processor-${each.region}
            forEach: nonexistent
            capabilities: [process]
        bindings:
          - name: trigger
            capability: process
            on:
              contextChange: {}
        """;
    assertThatThrownBy(() -> load(yaml))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nonexistent");
  }

  @Test
  void capabilityAlsoExpanded() throws Exception {
    var yaml =
        """
        name: test
        namespace: io.casehub.test
        version: "1.0"
        iterations:
          regions:
            in: [eu, us]
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
    var def = load(yaml);
    for (var w : def.getWorkers()) {
      assertThat(w.capabilities()).contains("process");
    }
  }
}
