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
package io.casehub.api.spi.routing;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record StepConsensusEntry(
    String bindingName,
    @Nullable String capabilityName,
    int occurrenceCount,
    int totalPlans,
    Map<String, Integer> workerDistribution,
    Map<String, Integer> outcomeDistribution,
    Map<Integer, Integer> priorityDistribution,
    List<String> contributingCaseIds,
    AgreementLevel agreement) {

  public StepConsensusEntry {
    Objects.requireNonNull(bindingName, "bindingName");
    if (occurrenceCount < 1) {
      throw new IllegalArgumentException("occurrenceCount must be >= 1");
    }
    if (totalPlans < 1) {
      throw new IllegalArgumentException("totalPlans must be >= 1");
    }
    workerDistribution = workerDistribution != null ? Map.copyOf(workerDistribution) : Map.of();
    outcomeDistribution = outcomeDistribution != null ? Map.copyOf(outcomeDistribution) : Map.of();
    priorityDistribution =
        priorityDistribution != null ? Map.copyOf(priorityDistribution) : Map.of();
    contributingCaseIds =
        contributingCaseIds != null ? List.copyOf(contributingCaseIds) : List.of();
    Objects.requireNonNull(agreement, "agreement");
  }
}
