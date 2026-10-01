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
package io.casehub.engine.yamlcbr;

import io.casehub.api.spi.routing.ExperiencePlanStep;
import io.casehub.yaml.plugin.api.PluginRegistry;
import io.casehub.yaml.step.catalog.ResolvedStep;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class CbrPlanToStepConverter {

  private final PluginRegistry catalog;

  public CbrPlanToStepConverter(PluginRegistry catalog) {
    this.catalog = catalog;
  }

  public List<ResolvedStep> convert(List<ExperiencePlanStep> planSteps) {
    return planSteps.stream()
        .filter(step -> !isExcluded(step.adaptationAction()))
        .sorted(Comparator.comparingInt(ExperiencePlanStep::priority))
        .map(this::toResolvedStep)
        .flatMap(Optional::stream)
        .toList();
  }

  private boolean isExcluded(@Nullable String adaptationAction) {
    return "REMOVED".equals(adaptationAction) || "SUPPRESSED".equals(adaptationAction);
  }

  private Optional<ResolvedStep> toResolvedStep(ExperiencePlanStep step) {
    if (step.capabilityName() == null) {
      return Optional.empty();
    }
    return catalog
        .resolve(step.capabilityName())
        .map(
            entry ->
                new ResolvedStep.PluginStep(
                    step.capabilityName(), entry, step.parameters(), Map.of()));
  }
}
