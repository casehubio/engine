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

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.stigmergy.ConvergenceState;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AllConvergedEvaluatorTest {

  AllConvergedEvaluator evaluator = new AllConvergedEvaluator(0.8);

  @Test
  void all_converged() {
    var result =
        evaluator.evaluate(
            Map.of(
                UUID.randomUUID(), new SwarmProgress(0.0, 0.9, 0.9, Instant.now()),
                UUID.randomUUID(), new SwarmProgress(0.0, 0.85, 0.85, Instant.now())));
    assertThat(result.state()).isEqualTo(ConvergenceState.CONSENSUS);
  }

  @Test
  void one_not_converged() {
    var result =
        evaluator.evaluate(
            Map.of(
                UUID.randomUUID(), new SwarmProgress(0.0, 0.9, 0.9, Instant.now()),
                UUID.randomUUID(), new SwarmProgress(0.0, 0.3, 0.3, Instant.now())));
    assertThat(result.state()).isEqualTo(ConvergenceState.PROGRESSING);
  }

  @Test
  void deadlock_detection() {
    var result =
        evaluator.evaluate(
            Map.of(
                UUID.randomUUID(), new SwarmProgress(0.0, 0.3, 0.9, Instant.now()),
                UUID.randomUUID(), new SwarmProgress(0.0, 0.9, 0.9, Instant.now())));
    assertThat(result.state()).isEqualTo(ConvergenceState.PROGRESSING);
  }

  @Test
  void empty_map_is_consensus() {
    var result = evaluator.evaluate(Map.of());
    assertThat(result.state()).isEqualTo(ConvergenceState.CONSENSUS);
  }
}
