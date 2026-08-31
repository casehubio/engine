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
    if (types == null) types = List.of();
    if (labels == null) labels = List.of();
    if (signals == null) signals = List.of();
    if (workers == null) workers = List.of();
    if (bindings == null) bindings = List.of();
    if (labelRules == null) labelRules = List.of();
    if (inboundMappings == null) inboundMappings = List.of();
    if (definitions == null) definitions = Map.of();
    if (iterations == null) iterations = Map.of();
  }
}
