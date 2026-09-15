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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StepConsensusEntryTest {

  @Test
  void valid_construction() {
    var entry =
        new StepConsensusEntry(
            "reduce-exposure",
            "risk-mitigation",
            3,
            5,
            Map.of("analyst-1", 2, "analyst-2", 1),
            Map.of("SUCCESS", 3),
            Map.of(1, 3),
            List.of("case-a", "case-b", "case-d"),
            AgreementLevel.CONSENSUS);
    assertEquals("reduce-exposure", entry.bindingName());
    assertEquals("risk-mitigation", entry.capabilityName());
    assertEquals(3, entry.occurrenceCount());
    assertEquals(5, entry.totalPlans());
    assertEquals(AgreementLevel.CONSENSUS, entry.agreement());
    assertEquals(List.of("case-a", "case-b", "case-d"), entry.contributingCaseIds());
    assertEquals(Map.of(1, 3), entry.priorityDistribution());
  }

  @Test
  void null_bindingName_rejected() {
    assertThrows(
        NullPointerException.class,
        () ->
            new StepConsensusEntry(
                null,
                null,
                1,
                1,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                AgreementLevel.UNANIMOUS));
  }

  @Test
  void occurrenceCount_below_one_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new StepConsensusEntry(
                "b",
                null,
                0,
                1,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                AgreementLevel.UNANIMOUS));
  }

  @Test
  void totalPlans_below_one_rejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new StepConsensusEntry(
                "b",
                null,
                1,
                0,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                AgreementLevel.UNANIMOUS));
  }

  @Test
  void maps_are_defensively_copied() {
    var workers = new HashMap<>(Map.of("a", 1));
    var entry =
        new StepConsensusEntry(
            "b", null, 1, 1, workers, Map.of(), Map.of(), List.of(), AgreementLevel.UNANIMOUS);
    workers.put("b", 2);
    assertEquals(1, entry.workerDistribution().size());
  }

  @Test
  void lists_are_defensively_copied() {
    var ids = new java.util.ArrayList<>(List.of("c1"));
    var entry =
        new StepConsensusEntry(
            "b", null, 1, 1, Map.of(), Map.of(), Map.of(), ids, AgreementLevel.UNANIMOUS);
    ids.add("c2");
    assertEquals(1, entry.contributingCaseIds().size());
  }

  @Test
  void nullable_capabilityName_accepted() {
    var entry =
        new StepConsensusEntry(
            "b", null, 1, 1, Map.of(), Map.of(), Map.of(), List.of(), AgreementLevel.UNIQUE);
    assertNull(entry.capabilityName());
  }

  @Test
  void null_agreement_rejected() {
    assertThrows(
        NullPointerException.class,
        () ->
            new StepConsensusEntry("b", null, 1, 1, Map.of(), Map.of(), Map.of(), List.of(), null));
  }
}
