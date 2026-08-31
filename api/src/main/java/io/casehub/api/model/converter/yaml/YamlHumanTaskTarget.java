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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.casehub.api.model.converter.deser.ExpressionEvaluatorDeserializer;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlHumanTaskTarget(
    String templateRef,
    String title,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator titleExpression,
    JsonNode candidateGroups,
    JsonNode candidateUsers,
    String expiresIn,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator expiresInExpression,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator expiresAtExpression,
    Integer claimDeadlineHours,
    String priority,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator inputMapping,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator outputMapping,
    String scope,
    @JsonDeserialize(using = ExpressionEvaluatorDeserializer.class)
        ExpressionEvaluator scopeExpression,
    Set<String> outcomes,
    String payloadType,
    String resolutionType,
    YamlQuorumConfig quorum) {

  public YamlHumanTaskTarget {
    if (outcomes == null) outcomes = Set.of();
  }
}
