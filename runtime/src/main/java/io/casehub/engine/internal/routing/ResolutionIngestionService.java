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
import io.casehub.api.spi.GuidanceStepInput;
import io.casehub.api.spi.ResolutionGuideInput;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.cbr.CbrCaseMemoryStore;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.casehub.neocortex.memory.cbr.GuidanceStep;
import io.casehub.neocortex.memory.cbr.ResolutionGuide;
import io.casehub.platform.api.path.Path;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ResolutionIngestionService {

  private static final Logger LOG = Logger.getLogger(ResolutionIngestionService.class);

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
    if (!adapterInstance.isResolvable() || !cbrStoreInstance.isResolvable()) {
      return;
    }
    CorpusSourceAdapter adapter = adapterInstance.get();
    CbrCaseMemoryStore store = cbrStoreInstance.get();
    List<ResolutionGuideInput> inputs = adapter.discover(tenancyId);
    int ingested = 0;
    int failed = 0;
    for (ResolutionGuideInput input : inputs) {
      try {
        ingestSingle(input, tenancyId, store);
        ingested++;
      } catch (Exception e) {
        failed++;
        LOG.warnf("Ingestion failed for document '%s': %s", input.documentId(), e.getMessage());
      }
    }
    if (ingested > 0 || failed > 0) {
      LOG.infof(
          "Corpus ingestion complete for tenant '%s': %d ingested, %d failed",
          tenancyId, ingested, failed);
    }
  }

  public void handleChange(CorpusChangeEvent event, String tenancyId) {
    if (!cbrStoreInstance.isResolvable()) {
      return;
    }
    CbrCaseMemoryStore store = cbrStoreInstance.get();
    switch (event) {
      case CorpusChangeEvent.Added a -> ingestSingle(a.input(), tenancyId, store);
      case CorpusChangeEvent.Updated u -> {
        String caseId = deterministicCaseId(u.documentId());
        store.supersedeAll(List.of(caseId), tenancyId, "corpus update");
        ingestSingle(u.input(), tenancyId, store);
      }
      case CorpusChangeEvent.Removed r -> {
        String caseId = deterministicCaseId(r.documentId());
        store.supersedeAll(List.of(caseId), tenancyId, "corpus removal");
      }
    }
  }

  private void ingestSingle(
      ResolutionGuideInput input, String tenancyId, CbrCaseMemoryStore store) {
    String caseId = deterministicCaseId(input.documentId());
    List<GuidanceStep> steps = mapSteps(input.steps());
    ResolutionGuide guide =
        new ResolutionGuide(
            input.problem(),
            input.solution(),
            null,
            null,
            mapFeatures(input.features()),
            steps,
            null,
            null);
    store.supersedeAll(List.of(caseId), tenancyId, "corpus re-ingestion");
    store.store(
        guide,
        ResolutionGuide.CBR_TYPE,
        input.documentId(),
        new MemoryDomain(input.domain()),
        tenancyId,
        caseId,
        Path.root());
  }

  private static List<GuidanceStep> mapSteps(List<GuidanceStepInput> steps) {
    if (steps == null || steps.isEmpty()) {
      return List.of();
    }
    return steps.stream()
        .map(
            s ->
                new GuidanceStep(
                    s.description(), s.preconditions(), s.expectedOutcome(), s.automationHint()))
        .toList();
  }

  private static Map<String, FeatureValue> mapFeatures(Map<String, Object> raw) {
    if (raw == null || raw.isEmpty()) {
      return Map.of();
    }
    var result = new java.util.LinkedHashMap<String, FeatureValue>();
    for (var entry : raw.entrySet()) {
      if (entry.getValue() instanceof FeatureValue fv) {
        result.put(entry.getKey(), fv);
      } else {
        result.put(entry.getKey(), FeatureValue.of(entry.getValue()));
      }
    }
    return Map.copyOf(result);
  }

  static String deterministicCaseId(String documentId) {
    return UUID.nameUUIDFromBytes(documentId.getBytes(StandardCharsets.UTF_8)).toString();
  }
}
