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
package io.casehub.api.spi.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.stigmergy.ConvergenceSignal;
import io.casehub.api.spi.stigmergy.ConvergenceState;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrossSwarmConvergenceEvaluatorTest {

  @Test
  void functional_interface_compiles() {
    CrossSwarmConvergenceEvaluator evaluator =
        progress -> new ConvergenceSignal(ConvergenceState.PROGRESSING, 0.0, "test");
    var result =
        evaluator.evaluate(
            Map.of(UUID.randomUUID(), new SwarmProgress(0.5, 0.5, 0.5, Instant.now())));
    assertThat(result.state()).isEqualTo(ConvergenceState.PROGRESSING);
  }
}
