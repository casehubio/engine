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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.runtime.engine.CaseHubRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlCaseHubDiscoveryTest {

  YamlCaseHubDiscovery discovery;
  CaseDefinitionRegistry caseDefinitionRegistry;
  CaseHubRegistry caseHubRegistry;
  CaseHubRuntime caseHubRuntime;

  @BeforeEach
  void setUp() {
    discovery = new YamlCaseHubDiscovery();
    caseDefinitionRegistry = mock(CaseDefinitionRegistry.class);
    caseHubRegistry = new CaseHubRegistry();
    caseHubRuntime = mock(CaseHubRuntime.class);
    discovery.caseDefinitionRegistry = caseDefinitionRegistry;
    discovery.caseHubRegistry = caseHubRegistry;
    discovery.caseHubRuntime = caseHubRuntime;
  }

  @Test
  void discover_registersHubForDefinitionWithoutOne() {
    CaseDefinition def = stubDefinition("yaml-only-case");
    when(caseDefinitionRegistry.allDefinitions()).thenReturn(List.of(def));

    discovery.discover();

    assertThat(caseHubRegistry.get("yaml-only-case")).isPresent();
    assertThat(caseHubRegistry.get("yaml-only-case").get().getDefinition()).isSameAs(def);
  }

  @Test
  void discover_skipsDefinitionThatAlreadyHasHub() {
    CaseDefinition def = stubDefinition("java-case");
    CaseHub existingHub =
        new CaseHub(caseHubRuntime) {
          @Override
          public CaseDefinition getDefinition() {
            return def;
          }
        };
    caseHubRegistry.register("java-case", existingHub);
    when(caseDefinitionRegistry.allDefinitions()).thenReturn(List.of(def));

    discovery.discover();

    assertThat(caseHubRegistry.get("java-case").get()).isSameAs(existingHub);
  }

  @Test
  void discover_handlesMultipleDefinitions() {
    CaseDefinition def1 = stubDefinition("case-a");
    CaseDefinition def2 = stubDefinition("case-b");
    CaseDefinition def3 = stubDefinition("case-c");
    CaseHub existingHub =
        new CaseHub(caseHubRuntime) {
          @Override
          public CaseDefinition getDefinition() {
            return def2;
          }
        };
    caseHubRegistry.register("case-b", existingHub);
    when(caseDefinitionRegistry.allDefinitions()).thenReturn(List.of(def1, def2, def3));

    discovery.discover();

    assertThat(caseHubRegistry.get("case-a")).isPresent();
    assertThat(caseHubRegistry.get("case-b").get()).isSameAs(existingHub);
    assertThat(caseHubRegistry.get("case-c")).isPresent();
    assertThat(caseHubRegistry.all()).hasSize(3);
  }

  @Test
  void discover_noDefinitions_doesNothing() {
    when(caseDefinitionRegistry.allDefinitions()).thenReturn(List.of());

    discovery.discover();

    assertThat(caseHubRegistry.all()).isEmpty();
  }

  private static CaseDefinition stubDefinition(String name) {
    CaseDefinition def = mock(CaseDefinition.class);
    when(def.getName()).thenReturn(name);
    return def;
  }
}
