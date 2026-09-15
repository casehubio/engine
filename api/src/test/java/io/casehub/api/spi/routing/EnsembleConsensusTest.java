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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnsembleConsensusTest {

  @Test
  void valid_step_level_construction() {
    var consensus =
        new EnsembleConsensus(ConsensusScope.STEP_LEVEL, List.of(), 0.72, 5, List.of("a", "b"));
    assertEquals(ConsensusScope.STEP_LEVEL, consensus.scope());
    assertEquals(0.72, consensus.ensembleConfidence(), 0.001);
    assertEquals(5, consensus.inputCount());
    assertEquals(List.of("a", "b"), consensus.sourceCaseIds());
  }

  @Test
  void valid_outcome_only_construction() {
    var consensus =
        new EnsembleConsensus(ConsensusScope.OUTCOME_ONLY, List.of(), 0.8, 3, List.of());
    assertEquals(ConsensusScope.OUTCOME_ONLY, consensus.scope());
    assertTrue(consensus.stepAnalysis().isEmpty());
  }

  @Test
  void confidence_below_zero_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EnsembleConsensus(ConsensusScope.STEP_LEVEL, List.of(), -0.1, 1, List.of()));
  }

  @Test
  void confidence_above_one_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EnsembleConsensus(ConsensusScope.STEP_LEVEL, List.of(), 1.1, 1, List.of()));
  }

  @Test
  void inputCount_negative_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new EnsembleConsensus(ConsensusScope.STEP_LEVEL, List.of(), 0.5, -1, List.of()));
  }

  @Test
  void null_scope_rejected() {
    assertThrows(
        NullPointerException.class,
        () -> new EnsembleConsensus(null, List.of(), 0.5, 1, List.of()));
  }

  @Test
  void lists_are_defensively_copied() {
    var ids = new ArrayList<>(List.of("a"));
    var consensus = new EnsembleConsensus(ConsensusScope.OUTCOME_ONLY, List.of(), 0.5, 1, ids);
    ids.add("b");
    assertEquals(1, consensus.sourceCaseIds().size());
  }

  @Test
  void boundary_confidence_zero_accepted() {
    var consensus =
        new EnsembleConsensus(ConsensusScope.OUTCOME_ONLY, List.of(), 0.0, 0, List.of());
    assertEquals(0.0, consensus.ensembleConfidence());
  }

  @Test
  void boundary_confidence_one_accepted() {
    var consensus = new EnsembleConsensus(ConsensusScope.STEP_LEVEL, List.of(), 1.0, 2, List.of());
    assertEquals(1.0, consensus.ensembleConfidence());
  }
}
