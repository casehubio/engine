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
package io.casehub.engine.spi;

import io.casehub.engine.internal.model.CaseInstance;
import io.smallrye.mutiny.Uni;
import java.util.UUID;

public interface CaseInstanceRepository {

  /** Persist a new case instance. Returns the saved instance with id populated. */
  Uni<CaseInstance> save(CaseInstance instance);

  /** Merge state changes back to storage (status transitions). */
  Uni<CaseInstance> update(CaseInstance instance);

  /**
   * Load a case instance by UUID with its CaseMetaModel eagerly joined. Returns null if not found.
   */
  Uni<CaseInstance> findByUuid(UUID uuid);
}
