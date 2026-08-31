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
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlAgentDescriptor(
    String agentId,
    String name,
    String slot,
    String tenancyId,
    String briefing,
    String version,
    String provider,
    String modelFamily,
    String modelVersion,
    String jurisdiction,
    String dataHandlingPolicy,
    List<YamlAgentGoal> goals,
    List<YamlAgentConstraint> constraints,
    YamlAgentDisposition disposition,
    List<YamlAgentCapability> capabilities) {

  public YamlAgentDescriptor {
    if (goals == null) goals = List.of();
    if (constraints == null) constraints = List.of();
    if (capabilities == null) capabilities = List.of();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record YamlAgentGoal(
      String name,
      String description,
      String priority,
      String visibility,
      List<String> capabilities,
      java.util.Map<String, String> attributes) {

    public YamlAgentGoal {
      if (capabilities == null) capabilities = List.of();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record YamlAgentConstraint(
      String name, String description, String visibility, String severity) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record YamlAgentDisposition(
      String socialOrient,
      String ruleFollowing,
      String riskAppetite,
      String autonomy,
      String conflictMode,
      Boolean delegation) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record YamlAgentCapability(
      String name,
      String description,
      Double qualityHint,
      Long latencyHintP50Ms,
      String costHint,
      List<String> tags) {

    public YamlAgentCapability {
      if (tags == null) tags = List.of();
    }
  }
}
