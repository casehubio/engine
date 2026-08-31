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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.casehub.api.model.Trigger;
import io.casehub.api.model.converter.deser.TriggerDeserializer;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlBinding(
    String name,
    String capability,
    @JsonDeserialize(using = TriggerDeserializer.class) Trigger on,
    ExpressionEvaluator when,
    ExpressionEvaluator inputProjectionOverride,
    JsonNode outcomePolicy,
    String conflictResolverStrategy,
    String lifecycleScope,
    String participation,
    String executionMode,
    @JsonAlias("replanAfter") String replanHint,
    JsonNode exchangeProjection,
    String produces,
    String consumes,
    List<String> producedKeys,
    List<String> contingency,
    Map<String, Object> contextWrite,
    Map<String, Object> signal,
    String sideEffectClassification,
    List<String> permissionIntent,
    YamlHumanTaskTarget humanTask,
    YamlJudgmentTarget judgment,
    YamlSubCaseTarget subCase,
    YamlRecoveryOverride recoveryOverride) {

  public YamlBinding {
    if (producedKeys == null) {
      producedKeys = List.of();
    }
    if (contingency == null) {
      contingency = List.of();
    }
    if (contextWrite == null) {
      contextWrite = Map.of();
    }
    if (permissionIntent == null) {
      permissionIntent = List.of();
    }
  }
}
