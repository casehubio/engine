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
package io.casehub.api.model.converter.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlCaseDefinition(
    String name,
    String namespace,
    String version,
    String dsl,
    String title,
    String summary,
    String expressionLang,
    String contextType,
    JsonNode context,
    Map<String, Object> semanticData,
    List<String> types,
    List<String> labels,
    List<YamlSignalType> signals,
    List<YamlContextLayer> layers,
    JsonNode episodic,
    JsonNode use,
    YamlCaseSpec spec,
    List<YamlWorker> workers,
    List<YamlBinding> bindings,
    List<YamlLabelRule> labelRules,
    List<YamlInboundMapping> inboundMappings,
    Map<String, JsonNode> definitions,
    Map<String, YamlIterationGroup> iterations) {

  public YamlCaseDefinition {
    if (types == null) {
      types = List.of();
    }
    if (labels == null) {
      labels = List.of();
    }
    if (signals == null) {
      signals = List.of();
    }
    if (layers == null) {
      layers = List.of();
    }
    if (workers == null) {
      workers = List.of();
    }
    if (bindings == null) {
      bindings = List.of();
    }
    if (labelRules == null) {
      labelRules = List.of();
    }
    if (inboundMappings == null) {
      inboundMappings = List.of();
    }
    if (definitions == null) {
      definitions = Map.of();
    }
    if (iterations == null) {
      iterations = Map.of();
    }
  }
}
