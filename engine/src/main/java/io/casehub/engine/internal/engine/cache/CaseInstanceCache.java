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
package io.casehub.engine.internal.engine.cache;

import io.casehub.engine.internal.model.CaseInstance;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CaseInstanceCache {

  private final Map<UUID, CaseInstance> cache = new ConcurrentHashMap<>();

  public void put(CaseInstance instance) {
    cache.put(instance.getUuid(), instance);
  }

  public CaseInstance get(UUID caseId) {
    return cache.get(caseId);
  }

  public void clear() {
    cache.clear();
  }

  /** Returns a snapshot of all currently cached CaseInstances for timeout scanning. */
  public java.util.Collection<CaseInstance> getAll() {
    return java.util.Collections.unmodifiableCollection(cache.values());
  }
}
