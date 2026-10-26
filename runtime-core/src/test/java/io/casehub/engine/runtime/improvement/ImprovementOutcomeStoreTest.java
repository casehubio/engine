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
package io.casehub.engine.runtime.improvement;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.improvement.ImprovementOutcome;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImprovementOutcomeStoreTest {

  private ImprovementOutcomeStore store;

  @BeforeEach
  void setUp() {
    store = new ImprovementOutcomeStore();
  }

  @Test
  void store_andRetrieve() {
    var outcome = outcome("lint-fix", "checkstyle", ImprovementOutcome.OutcomeStatus.MERGED);
    store.record(outcome);

    var history = store.findByCategory("lint-fix", 10);
    assertThat(history).hasSize(1);
    assertThat(history.get(0).status()).isEqualTo(ImprovementOutcome.OutcomeStatus.MERGED);
  }

  @Test
  void failureRate_allSuccessful() {
    store.record(outcome("lint-fix", "checkstyle", ImprovementOutcome.OutcomeStatus.MERGED));
    store.record(outcome("lint-fix", "checkstyle", ImprovementOutcome.OutcomeStatus.MERGED));

    assertThat(store.failureRate("lint-fix", "checkstyle")).isEqualTo(0.0);
  }

  @Test
  void failureRate_allFailed() {
    store.record(outcome("lint-fix", "checkstyle", ImprovementOutcome.OutcomeStatus.FAILED));
    store.record(outcome("lint-fix", "checkstyle", ImprovementOutcome.OutcomeStatus.REJECTED));

    assertThat(store.failureRate("lint-fix", "checkstyle")).isEqualTo(1.0);
  }

  @Test
  void failureRate_mixed() {
    store.record(outcome("dep-update", "hibernate", ImprovementOutcome.OutcomeStatus.MERGED));
    store.record(outcome("dep-update", "hibernate", ImprovementOutcome.OutcomeStatus.FAILED));
    store.record(outcome("dep-update", "hibernate", ImprovementOutcome.OutcomeStatus.MERGED));

    assertThat(store.failureRate("dep-update", "hibernate"))
        .isCloseTo(0.333, org.assertj.core.data.Offset.offset(0.01));
  }

  @Test
  void failureRate_noHistory() {
    assertThat(store.failureRate("unknown", "unknown")).isEqualTo(0.0);
  }

  @Test
  void reset_clearsAll() {
    store.record(outcome("lint-fix", "checkstyle", ImprovementOutcome.OutcomeStatus.MERGED));
    store.reset();

    assertThat(store.findByCategory("lint-fix", 10)).isEmpty();
  }

  private ImprovementOutcome outcome(
      String category, String target, ImprovementOutcome.OutcomeStatus status) {
    return new ImprovementOutcome(
        UUID.randomUUID(),
        UUID.randomUUID(),
        category,
        target,
        status,
        null,
        null,
        null,
        null,
        Instant.now(),
        Map.of());
  }
}
