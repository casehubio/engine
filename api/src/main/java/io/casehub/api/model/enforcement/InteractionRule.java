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
package io.casehub.api.model.enforcement;

import jakarta.annotation.Nullable;
import java.util.Objects;

public record InteractionRule(
    String ruleId, String description, RuleSeverity severity, @Nullable String constraint) {

  public InteractionRule {
    Objects.requireNonNull(ruleId, "ruleId must not be null");
    if (ruleId.isBlank()) throw new IllegalArgumentException("ruleId must not be blank");
    Objects.requireNonNull(description, "description must not be null");
    Objects.requireNonNull(severity, "severity must not be null");
  }

  public InteractionRule(String ruleId, String description, RuleSeverity severity) {
    this(ruleId, description, severity, null);
  }
}
