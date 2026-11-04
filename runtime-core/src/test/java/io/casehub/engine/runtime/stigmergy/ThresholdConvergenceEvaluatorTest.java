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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.stigmergy.ConvergenceState;
import io.casehub.api.spi.stigmergy.SwarmConvergenceEvaluator.ConvergenceContext;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ThresholdConvergenceEvaluatorTest {

  private final ThresholdConvergenceEvaluator evaluator = new ThresholdConvergenceEvaluator();
  private final UUID caseId = UUID.randomUUID();

  @Test
  void highConsensusSignalsConsensus() {
    var progress = new SwarmProgress(0.5, 0.9, 0.95, Instant.now());
    var context = new ConvergenceContext(10, SwarmProgress.EMPTY, 3);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.CONSENSUS, signal.state());
    assertTrue(signal.confidence() > 0.8);
  }

  @Test
  void highStabilityLowConsensusSignalsDeadlock() {
    var progress = new SwarmProgress(0.1, 0.1, 0.95, Instant.now());
    var context = new ConvergenceContext(10, SwarmProgress.EMPTY, 3);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.DEADLOCK, signal.state());
  }

  @Test
  void droppingExplorationSignalsDiminishingReturns() {
    var previous = new SwarmProgress(0.8, 0.3, 0.5, Instant.now());
    var progress = new SwarmProgress(0.2, 0.3, 0.5, Instant.now());
    var context = new ConvergenceContext(10, previous, 3);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.DIMINISHING_RETURNS, signal.state());
  }

  @Test
  void risingConsensusSignalsConverging() {
    var previous = new SwarmProgress(0.5, 0.3, 0.5, Instant.now());
    var progress = new SwarmProgress(0.5, 0.6, 0.7, Instant.now());
    var context = new ConvergenceContext(10, previous, 3);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.CONVERGING, signal.state());
  }

  @Test
  void earlyEvaluationSignalsProgressing() {
    var progress = new SwarmProgress(0.5, 0.3, 0.4, Instant.now());
    var context = new ConvergenceContext(2, SwarmProgress.EMPTY, 3);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.PROGRESSING, signal.state());
  }

  @Test
  void zeroAgentsSignalsProgressing() {
    var progress = new SwarmProgress(0.0, 0.0, 0.0, Instant.now());
    var context = new ConvergenceContext(5, SwarmProgress.EMPTY, 0);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.PROGRESSING, signal.state());
  }

  @Test
  void moderateScoresSignalProgressing() {
    var previous = new SwarmProgress(0.4, 0.4, 0.4, Instant.now());
    var progress = new SwarmProgress(0.5, 0.5, 0.5, Instant.now());
    var context = new ConvergenceContext(10, previous, 3);

    var signal = evaluator.evaluate(caseId, progress, context);

    assertEquals(ConvergenceState.PROGRESSING, signal.state());
  }
}
