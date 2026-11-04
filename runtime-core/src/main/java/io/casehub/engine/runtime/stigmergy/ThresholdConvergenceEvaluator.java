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
package io.casehub.engine.runtime.stigmergy;

import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.stigmergy.ConvergenceSignal;
import io.casehub.api.spi.stigmergy.ConvergenceState;
import io.casehub.api.spi.stigmergy.SwarmConvergenceEvaluator;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@DefaultBean
@ApplicationScoped
public class ThresholdConvergenceEvaluator implements SwarmConvergenceEvaluator {

  private static final double CONSENSUS_THRESHOLD = 0.8;
  private static final double STABILITY_THRESHOLD = 0.9;
  private static final double DEADLOCK_CONSENSUS_CEILING = 0.2;
  private static final double DIMINISHING_RETURNS_DROP = 0.4;
  private static final double CONVERGING_CONSENSUS_RISE = 0.15;
  private static final int MIN_EVALUATIONS_FOR_SIGNAL = 3;

  @Override
  public ConvergenceSignal evaluate(
      UUID caseId, SwarmProgress progress, ConvergenceContext context) {
    if (context.activeAgentCount() <= 0 || context.evaluationCount() < MIN_EVALUATIONS_FOR_SIGNAL) {
      return new ConvergenceSignal(ConvergenceState.PROGRESSING, 0.0, "insufficient data");
    }

    if (progress.consensusScore() >= CONSENSUS_THRESHOLD
        && progress.stabilityScore() >= STABILITY_THRESHOLD) {
      double confidence = (progress.consensusScore() + progress.stabilityScore()) / 2.0;
      return new ConvergenceSignal(
          ConvergenceState.CONSENSUS, confidence, "high consensus and stability");
    }

    if (progress.stabilityScore() >= STABILITY_THRESHOLD
        && progress.consensusScore() < DEADLOCK_CONSENSUS_CEILING) {
      return new ConvergenceSignal(
          ConvergenceState.DEADLOCK, progress.stabilityScore(), "stable roles without consensus");
    }

    var previous = context.previousProgress();
    double explorationDrop = previous.explorationPace() - progress.explorationPace();
    if (explorationDrop >= DIMINISHING_RETURNS_DROP) {
      return new ConvergenceSignal(
          ConvergenceState.DIMINISHING_RETURNS,
          explorationDrop,
          "exploration pace dropped significantly");
    }

    double consensusRise = progress.consensusScore() - previous.consensusScore();
    double stabilityRise = progress.stabilityScore() - previous.stabilityScore();
    if (consensusRise >= CONVERGING_CONSENSUS_RISE || stabilityRise >= CONVERGING_CONSENSUS_RISE) {
      double confidence = Math.max(consensusRise, stabilityRise);
      return new ConvergenceSignal(
          ConvergenceState.CONVERGING, confidence, "trending toward consensus");
    }

    return new ConvergenceSignal(
        ConvergenceState.PROGRESSING, 0.0, "no convergence signal detected");
  }
}
