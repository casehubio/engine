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
package io.casehub.platform.api.data.memory;

import io.casehub.platform.api.data.CollectionDescriptor;
import io.casehub.platform.api.data.DataCollection;
import io.casehub.platform.api.data.DataStore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore implements DataStore {

  private final Map<String, CollectionDescriptor> descriptors = new ConcurrentHashMap<>();
  private final Map<String, InMemoryDataCollection> collections = new ConcurrentHashMap<>();

  @Override
  public void register(CollectionDescriptor descriptor) {
    descriptors.put(descriptor.name(), descriptor);
  }

  @Override
  public DataCollection collection(String name) {
    return collections.computeIfAbsent(name, n -> new InMemoryDataCollection());
  }

  public void clear() {
    collections.values().forEach(InMemoryDataCollection::clearAll);
  }
}
