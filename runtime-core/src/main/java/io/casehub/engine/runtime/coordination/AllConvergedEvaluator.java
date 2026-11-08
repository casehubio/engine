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
package io.casehub.engine.runtime.coordination;

import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.coordination.CrossSwarmConvergenceEvaluator;
import io.casehub.api.spi.stigmergy.ConvergenceSignal;
import io.casehub.api.spi.stigmergy.ConvergenceState;
import java.util.Map;
import java.util.UUID;

public class AllConvergedEvaluator implements CrossSwarmConvergenceEvaluator {

  private final double threshold;

  public AllConvergedEvaluator(double threshold) {
    if (threshold < 0.0 || threshold > 1.0) {
      throw new IllegalArgumentException("threshold must be in [0.0, 1.0]");
    }
    this.threshold = threshold;
  }

  @Override
  public ConvergenceSignal evaluate(Map<UUID, SwarmProgress> swarmProgress) {
    if (swarmProgress.isEmpty()) {
      return new ConvergenceSignal(ConvergenceState.CONSENSUS, 1.0, "No swarms to evaluate");
    }
    boolean allConverged =
        swarmProgress.values().stream()
            .allMatch(p -> p.consensusScore() >= threshold && p.stabilityScore() >= threshold);
    if (allConverged) {
      double avgConsensus =
          swarmProgress.values().stream()
              .mapToDouble(SwarmProgress::consensusScore)
              .average()
              .orElse(0.0);
      return new ConvergenceSignal(
          ConvergenceState.CONSENSUS,
          avgConsensus,
          "All %d swarms converged".formatted(swarmProgress.size()));
    }
    long notConverged =
        swarmProgress.values().stream()
            .filter(p -> p.consensusScore() < threshold || p.stabilityScore() < threshold)
            .count();
    return new ConvergenceSignal(
        ConvergenceState.PROGRESSING,
        0.0,
        "%d of %d swarms still progressing".formatted(notConverged, swarmProgress.size()));
  }
}
