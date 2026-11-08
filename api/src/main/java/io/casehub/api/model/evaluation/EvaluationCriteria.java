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
package io.casehub.api.model.evaluation;

import jakarta.annotation.Nullable;
import java.util.Objects;

public record EvaluationCriteria(String criteriaId, String description, @Nullable Double weight) {

  public EvaluationCriteria {
    Objects.requireNonNull(criteriaId, "criteriaId must not be null");
    if (criteriaId.isBlank()) throw new IllegalArgumentException("criteriaId must not be blank");
    Objects.requireNonNull(description, "description must not be null");
  }

  public EvaluationCriteria(String criteriaId, String description) {
    this(criteriaId, description, null);
  }
}
