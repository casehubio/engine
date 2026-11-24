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
package io.casehub.api.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.casehub.api.model.CaseDefinition;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaseHubConstructorTest {

  @Test
  void constructorWithRuntime_delegatesStartCase() {
    CaseHubRuntime mockRuntime = mock(CaseHubRuntime.class);
    CaseDefinition mockDef = mock(CaseDefinition.class);
    UUID expectedId = UUID.randomUUID();
    when(mockRuntime.startCase(mockDef)).thenReturn(expectedId);

    CaseHub hub =
        new CaseHub(mockRuntime) {
          @Override
          public CaseDefinition getDefinition() {
            return mockDef;
          }
        };

    assertThat(hub.startCase()).isEqualTo(expectedId);
    verify(mockRuntime).startCase(mockDef);
  }

  @Test
  void constructorWithRuntime_delegatesSignal() {
    CaseHubRuntime mockRuntime = mock(CaseHubRuntime.class);
    CaseDefinition mockDef = mock(CaseDefinition.class);
    UUID caseId = UUID.randomUUID();

    CaseHub hub =
        new CaseHub(mockRuntime) {
          @Override
          public CaseDefinition getDefinition() {
            return mockDef;
          }
        };

    hub.signal(caseId, "$.status", "resolved");
    verify(mockRuntime).signal(caseId, "$.status", "resolved");
  }

  @Test
  void constructorWithRuntime_delegatesQuery() {
    CaseHubRuntime mockRuntime = mock(CaseHubRuntime.class);
    CaseDefinition mockDef = mock(CaseDefinition.class);
    UUID caseId = UUID.randomUUID();
    when(mockRuntime.query(caseId, "$.priority")).thenReturn("high");

    CaseHub hub =
        new CaseHub(mockRuntime) {
          @Override
          public CaseDefinition getDefinition() {
            return mockDef;
          }
        };

    assertThat(hub.query(caseId, "$.priority")).isEqualTo("high");
  }
}
