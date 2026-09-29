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
package io.casehub.api.spi;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record YamlStepExecutionEvent(
    UUID caseId,
    String tenancyId,
    String caseType,
    String actionName,
    long durationMs,
    boolean success,
    Map<String, Object> metadata,
    String bindingType,
    String resultClassification,
    @Nullable String executionEnvironment,
    @Nullable String parentStepName,
    @Nullable String playbookName,
    @Nullable String playbookVersion,
    @Nullable String traceId,
    Map<String, Object> contextSnapshot) {

  public YamlStepExecutionEvent {
    Objects.requireNonNull(caseId, "caseId");
    Objects.requireNonNull(tenancyId, "tenancyId");
    Objects.requireNonNull(caseType, "caseType");
    Objects.requireNonNull(actionName, "actionName");
    Objects.requireNonNull(bindingType, "bindingType");
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    contextSnapshot = contextSnapshot != null ? Map.copyOf(contextSnapshot) : Map.of();
  }
}
