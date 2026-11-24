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

import io.casehub.api.engine.CaseHub;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class CaseHubRegistry {

  private final ConcurrentMap<String, CaseHub> hubs = new ConcurrentHashMap<>();

  public void register(String name, CaseHub hub) {
    hubs.put(name, hub);
  }

  public Optional<CaseHub> get(String name) {
    return Optional.ofNullable(hubs.get(name));
  }

  public Collection<CaseHub> all() {
    return Collections.unmodifiableCollection(hubs.values());
  }
}
