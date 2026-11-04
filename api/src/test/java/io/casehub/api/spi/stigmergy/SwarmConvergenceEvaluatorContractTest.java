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
package io.casehub.api.spi.stigmergy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.stigmergy.SwarmConvergenceEvaluator.ConvergenceContext;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SwarmConvergenceEvaluatorContractTest {

  @Test
  void lambdaImplementationIsAccepted() {
    SwarmConvergenceEvaluator evaluator =
        (caseId, progress, context) ->
            new ConvergenceSignal(ConvergenceState.PROGRESSING, 0.5, "lambda");

    var signal =
        evaluator.evaluate(
            UUID.randomUUID(),
            new SwarmProgress(0.5, 0.5, 0.5, Instant.now()),
            new ConvergenceContext(1, SwarmProgress.EMPTY, 2));

    assertEquals(ConvergenceState.PROGRESSING, signal.state());
    assertEquals(0.5, signal.confidence());
    assertEquals("lambda", signal.reason());
  }

  @Test
  void convergenceContextExposesAllFields() {
    var previous = new SwarmProgress(0.1, 0.2, 0.3, Instant.now());
    var context = new ConvergenceContext(5, previous, 3);

    assertEquals(5, context.evaluationCount());
    assertEquals(previous, context.previousProgress());
    assertEquals(3, context.activeAgentCount());
  }

  @Test
  void convergenceSignalStaticProgressingConstant() {
    assertNotNull(ConvergenceSignal.PROGRESSING);
    assertEquals(ConvergenceState.PROGRESSING, ConvergenceSignal.PROGRESSING.state());
  }
}
