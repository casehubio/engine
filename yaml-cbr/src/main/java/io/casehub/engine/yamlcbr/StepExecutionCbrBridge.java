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
package io.casehub.engine.yamlcbr;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.spi.YamlStepExecutionEvent;
import io.casehub.api.spi.YamlStepExecutionObserver;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrPlanStep;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.casehub.platform.api.path.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

public class StepExecutionCbrBridge implements YamlStepExecutionObserver {

  private static final Logger LOG = Logger.getLogger(StepExecutionCbrBridge.class);

  private final CbrRecordStore cbrStore;
  private final CaseDefinitionRegistry registry;
  private final List<PlaybookContextExtractor> extractors;

  public StepExecutionCbrBridge(
      CbrRecordStore cbrStore,
      CaseDefinitionRegistry registry,
      List<PlaybookContextExtractor> extractors) {
    this.cbrStore = cbrStore;
    this.registry = registry;
    this.extractors = extractors;
  }

  @Override
  public void onYamlStepExecution(YamlStepExecutionEvent event) {
    try {
      doRecord(event);
    } catch (Exception e) {
      LOG.warnf(
          e,
          "CBR step recording failed for caseId=%s action=%s",
          event.caseId(),
          event.actionName());
    }
  }

  private void doRecord(YamlStepExecutionEvent event) {
    var defOpt = registry.findByName(event.caseType());
    if (defOpt.isEmpty()) {
      return;
    }
    CaseDefinition definition = defOpt.get();
    CbrConfig config = definition.getCbrConfig();
    if (config == null) {
      return;
    }

    Map<String, FeatureValue> features = buildFeatures(event, config);

    String outcome = event.success() ? "SUCCESS" : "FAILURE";
    String problem = event.actionName() + " [" + event.bindingType() + "]";
    String solution = event.resultClassification();

    var step =
        new CbrPlanStep(
            event.actionName(), event.actionName(), null, outcome, 0, event.metadata(), null);

    var record =
        new CbrPlanRecord(problem, solution, outcome, null, features, List.of(step), null, null);

    String domain = config.domain() != null ? config.domain() : definition.getName();
    cbrStore.store(
        record,
        event.caseType(),
        "yaml-step-" + event.actionName(),
        new MemoryDomain(domain),
        event.tenancyId(),
        event.caseId().toString(),
        Path.root());
  }

  private Map<String, FeatureValue> buildFeatures(YamlStepExecutionEvent event, CbrConfig config) {
    var features = new LinkedHashMap<String, FeatureValue>();

    features.put("actionName", FeatureValue.of(event.actionName()));
    features.put("durationMs", FeatureValue.of(event.durationMs()));
    features.put("bindingType", FeatureValue.of(event.bindingType()));
    features.put("success", FeatureValue.of(event.success()));

    if (event.executionEnvironment() != null) {
      features.put("executionEnvironment", FeatureValue.of(event.executionEnvironment()));
    }
    if (event.playbookName() != null) {
      features.put("playbookName", FeatureValue.of(event.playbookName()));
    }
    if (event.playbookVersion() != null) {
      features.put("playbookVersion", FeatureValue.of(event.playbookVersion()));
    }

    PlaybookContextExtractor extractor = findExtractor(config);
    if (extractor != null) {
      try {
        var domainFeatures = extractor.extract(event);
        if (domainFeatures != null) {
          features.putAll(domainFeatures);
        }
      } catch (Exception e) {
        LOG.warnf(e, "PlaybookContextExtractor failed for domain='%s'", config.domain());
      }
    }

    return Map.copyOf(features);
  }

  private PlaybookContextExtractor findExtractor(CbrConfig config) {
    for (PlaybookContextExtractor extractor : extractors) {
      if (extractor.supports(config)) {
        return extractor;
      }
    }
    return null;
  }
}
