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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CbrRetrievalResultTest {

  @Test
  void empty_factory() {
    var result = CbrRetrievalResult.empty();
    assertTrue(result.experiences().isEmpty());
    assertNull(result.ensemble());
  }

  @Test
  void null_experiences_rejected() {
    assertThrows(NullPointerException.class, () -> new CbrRetrievalResult(null, null));
  }

  @Test
  void null_ensemble_accepted() {
    var result = new CbrRetrievalResult(List.of(), null);
    assertNull(result.ensemble());
  }

  @Test
  void experiences_defensively_copied() {
    var list = new java.util.ArrayList<RetrievedExperience>();
    var result = new CbrRetrievalResult(list, null);
    assertThrows(UnsupportedOperationException.class, () -> result.experiences().add(null));
  }

  @Test
  void with_ensemble() {
    var ensemble = new EnsembleConsensus(ConsensusScope.OUTCOME_ONLY, List.of(), 0.8, 3, List.of());
    var result = new CbrRetrievalResult(List.of(), ensemble);
    assertNotNull(result.ensemble());
    assertEquals(ConsensusScope.OUTCOME_ONLY, result.ensemble().scope());
  }
}
