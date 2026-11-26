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
package io.casehub.engine.persistence.json.quarkus;

import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.persistence.json.JsonCollectionCaseInstanceCache;
import io.casehub.pages.data.json.JsonCollectionStore;
import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class PersistenceJsonBeans {

  @Produces
  @Alternative
  @Priority(200)
  @ApplicationScoped
  CaseInstanceCache jsonCollectionCaseInstanceCache(
      JsonCollectionStore store, CaseDefinitionRegistry registry, CurrentPrincipal principal) {
    return new JsonCollectionCaseInstanceCache(store, registry, principal);
  }
}
