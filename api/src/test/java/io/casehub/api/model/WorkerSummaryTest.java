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
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkerSummaryTest {

  @Test
  void happyPath_allFieldsAccessible() {
    var id = UUID.randomUUID();
    var start = Instant.parse("2026-04-23T10:00:00Z");
    var end = Instant.parse("2026-04-23T10:15:00Z");
    var s = new WorkerSummary("worker-1", "alice", start, end, "produced auth-map", id);
    assertThat(s.workerId()).isEqualTo("worker-1");
    assertThat(s.workerName()).isEqualTo("alice");
    assertThat(s.startedAt()).isEqualTo(start);
    assertThat(s.completedAt()).isEqualTo(end);
    assertThat(s.outputSummary()).isEqualTo("produced auth-map");
    assertThat(s.ledgerEntryId()).isEqualTo(id);
  }

  @Test
  void nullLedgerEntryId_isPermitted() {
    var s = new WorkerSummary("w", "n", Instant.now(), Instant.now(), "out", null);
    assertThat(s.ledgerEntryId()).isNull();
  }

  @Test
  void nullOutputSummary_isPermitted() {
    var s = new WorkerSummary("w", "n", Instant.now(), Instant.now(), null, UUID.randomUUID());
    assertThat(s.outputSummary()).isNull();
  }
}
