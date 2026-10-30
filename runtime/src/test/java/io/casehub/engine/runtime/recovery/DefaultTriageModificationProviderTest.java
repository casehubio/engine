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
package io.casehub.engine.runtime.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.common.spi.recovery.BindingRecoveryState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultTriageModificationProviderTest {

  private CaseRecoveryStateRegistry stateRegistry;
  private DefaultTriageModificationProvider provider;

  @BeforeEach
  void setUp() {
    stateRegistry = new CaseRecoveryStateRegistry();
    provider = new DefaultTriageModificationProvider(stateRegistry);
  }

  @Test
  void returnsModificationsForKnownBinding() {
    UUID caseId = UUID.randomUUID();
    BindingRecoveryState bindingState = stateRegistry.getOrCreate(caseId).getOrCreate("worker-a");
    bindingState.recordTriageModifications(Map.of("playbook.skipSteps", List.of("step-1")));

    List<Map<String, Object>> result = provider.getModifications(caseId, "worker-a");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst()).containsKey("playbook.skipSteps");
  }

  @Test
  void returnsEmptyForUnknownBinding() {
    UUID caseId = UUID.randomUUID();
    stateRegistry.getOrCreate(caseId);

    assertThat(provider.getModifications(caseId, "unknown")).isEmpty();
  }

  @Test
  void returnsEmptyWhenNoTriageState() {
    assertThat(provider.getModifications(UUID.randomUUID(), "any")).isEmpty();
  }
}
