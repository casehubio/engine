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
package io.casehub.engine.runtime.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.model.CaseDefinition;
import org.junit.jupiter.api.Test;

class CaseHubRegistryTest {

  @Test
  void registerAndRetrieve() {
    CaseHubRegistry registry = new CaseHubRegistry();
    CaseHub hub = createStubHub("test-case");

    registry.register("test-case", hub);

    assertThat(registry.get("test-case")).isPresent().contains(hub);
  }

  @Test
  void getUnknown_returnsEmpty() {
    CaseHubRegistry registry = new CaseHubRegistry();
    assertThat(registry.get("nonexistent")).isEmpty();
  }

  @Test
  void all_returnsAllRegistered() {
    CaseHubRegistry registry = new CaseHubRegistry();
    registry.register("case-a", createStubHub("case-a"));
    registry.register("case-b", createStubHub("case-b"));

    assertThat(registry.all()).hasSize(2);
  }

  @Test
  void duplicateRegistration_secondWins() {
    CaseHubRegistry registry = new CaseHubRegistry();
    CaseHub hub1 = createStubHub("same-name");
    CaseHub hub2 = createStubHub("same-name");

    registry.register("same-name", hub1);
    registry.register("same-name", hub2);

    assertThat(registry.get("same-name")).isPresent().contains(hub2);
  }

  private static CaseHub createStubHub(String name) {
    CaseHubRuntime runtime = mock(CaseHubRuntime.class);
    CaseDefinition def = mock(CaseDefinition.class);
    when(def.getName()).thenReturn(name);
    return new CaseHub(runtime) {
      @Override
      public CaseDefinition getDefinition() {
        return def;
      }
    };
  }
}
