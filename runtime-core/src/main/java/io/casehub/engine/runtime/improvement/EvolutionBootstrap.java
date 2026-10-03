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
package io.casehub.engine.runtime.improvement;

import io.casehub.api.spi.improvement.ConflictStrategy;
import io.casehub.api.spi.improvement.DenyPatternProvider;
import io.casehub.api.spi.improvement.ImprovementCategoryProvider;
import io.casehub.api.spi.improvement.ImprovementProposalSource;
import io.casehub.api.spi.improvement.RegressionEvaluator;

public class EvolutionBootstrap {

  public EvolutionBootstrap(
      ImprovementCategoryRegistry categoryRegistry,
      ImprovementProposalSourceRegistry proposalSourceRegistry,
      RegressionEvaluatorRegistry regressionEvaluatorRegistry,
      ConflictStrategyRegistry conflictStrategyRegistry,
      DenyPatternProviderRegistry denyPatternProviderRegistry,
      java.util.List<ImprovementCategoryProvider> categoryProviders,
      java.util.List<ImprovementProposalSource> proposalSources,
      java.util.List<RegressionEvaluator> regressionEvaluators,
      java.util.List<ConflictStrategy> conflictStrategies,
      java.util.List<DenyPatternProvider> denyPatternProviders) {
    for (var provider : categoryProviders) {
      categoryRegistry.registerProvider(provider);
    }
    for (var source : proposalSources) {
      proposalSourceRegistry.register(source);
    }
    for (var evaluator : regressionEvaluators) {
      regressionEvaluatorRegistry.register(evaluator);
    }
    for (var strategy : conflictStrategies) {
      conflictStrategyRegistry.register(strategy);
    }
    for (var provider : denyPatternProviders) {
      denyPatternProviderRegistry.register(provider);
    }
  }
}
