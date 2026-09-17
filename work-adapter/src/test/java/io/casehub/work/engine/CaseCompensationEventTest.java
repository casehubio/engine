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
package io.casehub.work.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CaseCompensationEventTest {

  @Test
  void type_returnsCorrectPrefix_forStarted() {
    var event =
        new CaseCompensationEvent(
            CaseCompensationEvent.Kind.STARTED,
            "tenant-1",
            UUID.randomUUID(),
            "ClinicalTrial",
            "COMPENSATING",
            "operator-1");
    assertThat(event.type()).isEqualTo("io.casehub.engine.case.compensation.started");
  }

  @Test
  void type_returnsCorrectPrefix_forCompleted() {
    var event =
        new CaseCompensationEvent(
            CaseCompensationEvent.Kind.COMPLETED,
            "tenant-1",
            UUID.randomUUID(),
            "ClinicalTrial",
            "COMPENSATED",
            "operator-1");
    assertThat(event.type()).isEqualTo("io.casehub.engine.case.compensation.completed");
  }

  @Test
  void type_returnsCorrectPrefix_forFaulted() {
    var event =
        new CaseCompensationEvent(
            CaseCompensationEvent.Kind.FAULTED,
            "tenant-1",
            UUID.randomUUID(),
            "ClinicalTrial",
            "COMPENSATION_FAULTED",
            "operator-1");
    assertThat(event.type()).isEqualTo("io.casehub.engine.case.compensation.faulted");
  }

  @Test
  void tenancyId_returnsConstructionValue() {
    var event =
        new CaseCompensationEvent(
            CaseCompensationEvent.Kind.STARTED,
            "my-tenant",
            UUID.randomUUID(),
            null,
            "COMPENSATING",
            null);
    assertThat(event.tenancyId()).isEqualTo("my-tenant");
  }

  @Test
  void rejectsNullKind() {
    assertThatThrownBy(
            () -> new CaseCompensationEvent(null, "t", UUID.randomUUID(), null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullTenancyId() {
    assertThatThrownBy(
            () ->
                new CaseCompensationEvent(
                    CaseCompensationEvent.Kind.STARTED, null, UUID.randomUUID(), null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullCaseId() {
    assertThatThrownBy(
            () ->
                new CaseCompensationEvent(
                    CaseCompensationEvent.Kind.STARTED, "t", null, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }
}
