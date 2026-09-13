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
package io.casehub.engine.internal.routing;

import io.casehub.api.spi.CorpusChangeEvent;
import io.casehub.api.spi.CorpusSourceAdapter;
import io.casehub.neocortex.memory.cbr.CbrCaseMemoryStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

// TODO #1081: restore corpus ingestion when neocortex-memory-api publishes
// GuidanceStep and ResolutionGuide gains steps/features constructor
@ApplicationScoped
public class ResolutionIngestionService {

  private final Instance<CorpusSourceAdapter> adapterInstance;
  private final Instance<CbrCaseMemoryStore> cbrStoreInstance;

  @Inject
  public ResolutionIngestionService(
      Instance<CorpusSourceAdapter> adapterInstance,
      Instance<CbrCaseMemoryStore> cbrStoreInstance) {
    this.adapterInstance = adapterInstance;
    this.cbrStoreInstance = cbrStoreInstance;
  }

  public void ingest(String tenancyId) {
    // no-op until neocortex-memory-api publishes GuidanceStep + updated ResolutionGuide
  }

  public void handleChange(CorpusChangeEvent event, String tenancyId) {
    // no-op until neocortex-memory-api publishes GuidanceStep + updated ResolutionGuide
  }

  static String deterministicCaseId(String documentId) {
    return java.util
        .UUID
        .nameUUIDFromBytes(documentId.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        .toString();
  }
}
