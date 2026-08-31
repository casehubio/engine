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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlJudgmentTarget(
    String prompt,
    String promptExpression,
    String inputMapping,
    String outputMapping,
    String resolutionType,
    String expiresIn,
    String expiresInExpression,
    String expiresAtExpression,
    List<String> evidenceRequirements,
    String verifierStrategy,
    @JsonAlias("escalatorStrategy") String escalationStrategy,
    String trustThreshold,
    String title,
    String titleExpression,
    Set<String> outcomes,
    String scope,
    String scopeExpression,
    String priority,
    JsonNode human,
    JsonNode callerConfig,
    Integer maxEscalationAttempts) {

  public YamlJudgmentTarget {
    if (evidenceRequirements == null) {
      evidenceRequirements = List.of();
    }
    if (outcomes == null) {
      outcomes = Set.of();
    }
  }
}
