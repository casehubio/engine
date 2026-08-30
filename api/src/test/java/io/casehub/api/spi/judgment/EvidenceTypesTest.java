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
package io.casehub.api.spi.judgment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EvidenceTypesTest {

  @Test
  void evidenceRequirementRequiredFactory() {
    var req = EvidenceRequirement.required("signature", EvidenceType.SIGNATURE);
    assertEquals("signature", req.key());
    assertEquals(EvidenceType.SIGNATURE, req.type());
    assertTrue(req.required());
  }

  @Test
  void evidenceRequirementOptionalFactory() {
    var req = EvidenceRequirement.optional("notes", EvidenceType.DOCUMENT);
    assertEquals("notes", req.key());
    assertEquals(EvidenceType.DOCUMENT, req.type());
    assertFalse(req.required());
  }

  @Test
  void evidenceRequirementTwoArgConstructorDefaultsToRequired() {
    var req = new EvidenceRequirement("metric", EvidenceType.METRIC);
    assertTrue(req.required());
  }

  @Test
  void evidenceFactory() {
    var ev = Evidence.of("confidence_score", EvidenceType.METRIC, 0.95);
    assertEquals("confidence_score", ev.key());
    assertEquals(EvidenceType.METRIC, ev.type());
    assertEquals(0.95, ev.value());
  }

  @Test
  void evidenceTypeValues() {
    assertEquals(6, EvidenceType.values().length);
    assertNotNull(EvidenceType.valueOf("ATTESTATION"));
    assertNotNull(EvidenceType.valueOf("DOCUMENT"));
    assertNotNull(EvidenceType.valueOf("SIGNATURE"));
    assertNotNull(EvidenceType.valueOf("REASONING"));
    assertNotNull(EvidenceType.valueOf("METRIC"));
    assertNotNull(EvidenceType.valueOf("EXTERNAL_REFERENCE"));
  }
}
