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
package io.casehub.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FailureDiagnosisTest {

  @Test
  void construction_and_access() {
    var now = Instant.now();
    var diagnosis =
        FailureDiagnosis.of(new FailureCategory.Transient("timeout"), "worker-1", "EXPIRED", now);
    assertThat(diagnosis.category().categoryName()).isEqualTo("transient");
    assertThat(diagnosis.workerId()).isEqualTo("worker-1");
    assertThat(diagnosis.outcomeStatus()).isEqualTo("EXPIRED");
    assertThat(diagnosis.timestamp()).isEqualTo(now);
  }

  @Test
  void storesCritique() {
    var diag =
        new FailureDiagnosis(
            new FailureCategory.Knowledge("missing data", "accountId"),
            "worker-1",
            "DECLINED",
            Instant.now(),
            "Entity resolution failed because input lacked account identifier format");
    assertThat(diag.critique())
        .isEqualTo("Entity resolution failed because input lacked account identifier format");
  }

  @Test
  void ofFactoryDefaultsCritiqueToNull() {
    var diag =
        FailureDiagnosis.of(
            new FailureCategory.Transient("timeout"), "worker-1", "EXPIRED", Instant.now());
    assertThat(diag.critique()).isNull();
  }

  @Test
  void fiveArgConstructorAcceptsNullCritique() {
    var diag =
        new FailureDiagnosis(
            new FailureCategory.Transient("timeout"), "worker-1", "EXPIRED", Instant.now(), null);
    assertThat(diag.critique()).isNull();
  }
}
