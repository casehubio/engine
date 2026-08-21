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
package io.casehub.api.model;

import java.util.Objects;

public record AdaptationConfig(
    String trigger, String optimization, Double threshold, String metaReasoner, String repair) {

  public static final double DEFAULT_PROGRESS_THRESHOLD = 0.3;

  public AdaptationConfig {
    Objects.requireNonNull(trigger, "trigger");
    Objects.requireNonNull(optimization, "optimization");
    if (threshold != null && (threshold < 0.0 || threshold > 1.0)) {
      throw new IllegalArgumentException("threshold must be in [0.0, 1.0]");
    }
  }

  public static AdaptationConfig of(String trigger, String optimization) {
    return new AdaptationConfig(trigger, optimization, null, null, null);
  }

  public String effectiveRepair(CaseDefinition definition) {
    if (repair != null) {
      return repair;
    }
    if ("goap".equals(definition.getDecompositionStrategy())) {
      return "goap-repair";
    }
    return "llm-repair";
  }

  public String effectiveMetaReasoner() {
    return metaReasoner != null ? metaReasoner : "cost-ceiling";
  }
}
