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
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlCompound(
    String name,
    String completionSemantics,
    String dispatchMode,
    List<String> children,
    Map<String, String> scopedBindings,
    ExpressionEvaluator entryCondition,
    ExpressionEvaluator exitCondition,
    Boolean repeatable,
    Integer mofnRequired,
    String planningStrategy) {

  public YamlCompound {
    if (children == null) children = List.of();
    if (scopedBindings == null) scopedBindings = Map.of();
  }
}
