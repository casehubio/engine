package io.casehub.api.model.converter.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlWorker(
    String name,
    String description,
    String definitionRef,
    List<String> capabilities,
    String contextType,
    String outputType,
    List<String> sequence,
    YamlExecutionPolicy executionPolicy,
    String forEach,
    YamlAgent agent,
    YamlReact react,
    YamlA2A a2a,
    YamlMcp mcp,
    @JsonProperty("do") JsonNode doBlock,
    Double cost,
    Map<String, Boolean> effect,
    List<String> softDependency,
    YamlAgentDescriptor agentDescriptor) {

  public YamlWorker {
    if (capabilities == null) capabilities = List.of();
    if (sequence == null) sequence = List.of();
    if (effect == null) effect = Map.of();
    if (softDependency == null) softDependency = List.of();
  }
}
