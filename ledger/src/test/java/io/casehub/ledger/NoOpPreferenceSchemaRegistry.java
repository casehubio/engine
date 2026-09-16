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
package io.casehub.ledger;

import io.casehub.platform.api.preferences.PreferenceSchemaDescriptor;
import io.casehub.platform.api.preferences.PreferenceSchemaRegistry;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.Set;

@DefaultBean
@ApplicationScoped
public class NoOpPreferenceSchemaRegistry implements PreferenceSchemaRegistry {

  @Override
  public void register(PreferenceSchemaDescriptor descriptor) {}

  @Override
  public Optional<PreferenceSchemaDescriptor> resolve(String key) {
    return Optional.empty();
  }

  @Override
  public Set<PreferenceSchemaDescriptor> discover() {
    return Set.of();
  }
}
