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
package io.casehub.engine.internal.engine.recovery;

import io.casehub.api.context.CaseContext;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.recovery.CaseContextRecoveryStrategy;
import io.casehub.engine.internal.context.CaseContextImpl;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default recovery strategy — O(1) recovery from a persisted context snapshot. The snapshot is
 * serialised by {@link #onContextChanged} and stored on the {@link CaseInstance} as a {@code
 * JsonNode} field that the repository persists within the existing transaction.
 */
@ApplicationScoped
public class SnapshotRecoveryStrategy implements CaseContextRecoveryStrategy {

  @Override
  public CaseContext recover(CaseInstance instance) {
    var snapshot = instance.getContextSnapshot();
    if (snapshot == null || snapshot.isNull()) {
      throw new IllegalStateException(
          "No context snapshot for caseId="
              + instance.getUuid()
              + " — snapshot recovery requires a persisted snapshot");
    }
    return CaseContextImpl.fromLayerDocument(snapshot);
  }

  @Override
  public void onContextChanged(CaseInstance instance, CaseContext context) {
    instance.setContextSnapshot(context.asJsonNode());
  }
}
