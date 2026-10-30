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

import io.casehub.engine.common.spi.recovery.BindingRecoveryState;
import io.casehub.engine.common.spi.recovery.CaseRecoveryState;
import io.casehub.engine.common.spi.recovery.TriageModificationProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DefaultTriageModificationProvider implements TriageModificationProvider {

  private final CaseRecoveryStateRegistry stateRegistry;

  @Inject
  public DefaultTriageModificationProvider(CaseRecoveryStateRegistry stateRegistry) {
    this.stateRegistry = stateRegistry;
  }

  @Override
  public List<Map<String, Object>> getModifications(UUID caseId, String bindingName) {
    CaseRecoveryState caseState = stateRegistry.find(caseId);
    if (caseState == null) {
      return List.of();
    }
    BindingRecoveryState bindingState = caseState.find(bindingName);
    if (bindingState == null) {
      return List.of();
    }
    return bindingState.previousTriageModifications();
  }
}
