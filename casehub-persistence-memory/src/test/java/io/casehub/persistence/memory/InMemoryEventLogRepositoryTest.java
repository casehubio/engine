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
package io.casehub.persistence.memory;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryEventLogRepositoryTest {

  InMemoryEventLogRepository repository;

  @BeforeEach
  void setUp() {
    repository = new InMemoryEventLogRepository();
  }

  // --- Happy path ---

  @Test
  void append_populatesIdAndSeq() {
    EventLog log = event(UUID.randomUUID(), "worker-1", CaseHubEventType.WORKER_SCHEDULED);

    repository.append(log).await().indefinitely();

    assertThat(log.id).isNotNull().isPositive();
    assertThat(log.getSeq()).isNotNull().isPositive();
  }

  @Test
  void append_seqIsMonotonicallyIncreasing() {
    UUID caseId = UUID.randomUUID();
    EventLog first = event(caseId, "w-1", CaseHubEventType.WORKER_SCHEDULED);
    EventLog second = event(caseId, "w-2", CaseHubEventType.WORKER_SCHEDULED);

    repository.append(first).await().indefinitely();
    repository.append(second).await().indefinitely();

    assertThat(second.getSeq()).isGreaterThan(first.getSeq());
  }

  @Test
  void appendAndReturnId_returnsIdAndPopulatesLog() {
    EventLog log = event(UUID.randomUUID(), "worker-ret", CaseHubEventType.WORKER_SCHEDULED);

    Long returned = repository.appendAndReturnId(log).await().indefinitely();

    assertThat(returned).isNotNull().isPositive();
    assertThat(returned).isEqualTo(log.id);
    assertThat(log.getSeq()).isNotNull().isPositive();
  }

  @Test
  void findById_returnsAppendedEvent() {
    UUID caseId = UUID.randomUUID();
    EventLog log = event(caseId, "worker-find", CaseHubEventType.WORKER_SCHEDULED);
    repository.append(log).await().indefinitely();

    EventLog found = repository.findById(log.id).await().indefinitely();

    assertThat(found).isNotNull();
    assertThat(found.getCaseId()).isEqualTo(caseId);
    assertThat(found.getEventType()).isEqualTo(CaseHubEventType.WORKER_SCHEDULED);
    assertThat(found.getWorkerId()).isEqualTo("worker-find");
  }

  @Test
  void findSchedulingEvents_returnsScheduledStartedAndCompleted() {
    UUID caseId = UUID.randomUUID();
    String workerId = "worker-sched-" + UUID.randomUUID();

    EventLog scheduled = event(caseId, workerId, CaseHubEventType.WORKER_SCHEDULED);
    EventLog started = event(caseId, workerId, CaseHubEventType.WORKER_EXECUTION_STARTED);
    EventLog otherWorker = event(caseId, "other", CaseHubEventType.WORKER_SCHEDULED);
    EventLog otherCase = event(UUID.randomUUID(), workerId, CaseHubEventType.WORKER_SCHEDULED);

    repository.append(scheduled).await().indefinitely();
    repository.append(started).await().indefinitely();
    repository.append(otherWorker).await().indefinitely();
    repository.append(otherCase).await().indefinitely();

    List<EventLog> result =
        repository.findSchedulingEvents(caseId, workerId).await().indefinitely();

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(EventLog::getEventType)
        .containsExactlyInAnyOrder(
            CaseHubEventType.WORKER_SCHEDULED, CaseHubEventType.WORKER_EXECUTION_STARTED);
  }

  @Test
  void findByTypes_returnsMatchingEventsOrderedBySeq() {
    UUID caseId = UUID.randomUUID();
    EventLog e1 = event(caseId, "w", CaseHubEventType.CASE_STARTED);
    EventLog e2 = event(caseId, "w", CaseHubEventType.WORKER_EXECUTION_COMPLETED);
    EventLog noise = event(caseId, "w", CaseHubEventType.WORKER_SCHEDULED);

    repository.append(e1).await().indefinitely();
    repository.append(e2).await().indefinitely();
    repository.append(noise).await().indefinitely();

    List<EventLog> result =
        repository
            .findByTypes(
                List.of(CaseHubEventType.CASE_STARTED, CaseHubEventType.WORKER_EXECUTION_COMPLETED))
            .await()
            .indefinitely();

    assertThat(result).hasSize(2);
    assertThat(result.stream().map(EventLog::getSeq).toList()).isSorted();
    assertThat(result.stream().map(EventLog::getEventType).toList())
        .doesNotContain(CaseHubEventType.WORKER_SCHEDULED);
  }

  @Test
  void findByCaseAndTypes_filtersByCaseId() {
    UUID target = UUID.randomUUID();
    UUID other = UUID.randomUUID();

    repository.append(event(target, "w", CaseHubEventType.CASE_STARTED)).await().indefinitely();
    repository.append(event(other, "w", CaseHubEventType.CASE_STARTED)).await().indefinitely();

    List<EventLog> result =
        repository
            .findByCaseAndTypes(target, List.of(CaseHubEventType.CASE_STARTED))
            .await()
            .indefinitely();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCaseId()).isEqualTo(target);
    assertThat(result.stream().map(EventLog::getSeq).toList()).isSorted();
  }

  @Test
  void findByCaseAndWorkerAndType_filtersAllThreeDimensions() {
    UUID caseId = UUID.randomUUID();
    String workerId = "worker-filter-" + UUID.randomUUID();

    EventLog match = event(caseId, workerId, CaseHubEventType.WORKER_EXECUTION_FAILED);
    EventLog wrongWorker = event(caseId, "other", CaseHubEventType.WORKER_EXECUTION_FAILED);
    EventLog wrongType = event(caseId, workerId, CaseHubEventType.WORKER_SCHEDULED);

    repository.append(match).await().indefinitely();
    repository.append(wrongWorker).await().indefinitely();
    repository.append(wrongType).await().indefinitely();

    List<EventLog> result =
        repository
            .findByCaseAndWorkerAndType(caseId, workerId, CaseHubEventType.WORKER_EXECUTION_FAILED)
            .await()
            .indefinitely();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWorkerId()).isEqualTo(workerId);
    assertThat(result.get(0).getEventType()).isEqualTo(CaseHubEventType.WORKER_EXECUTION_FAILED);
  }

  // --- Edge cases ---

  @Test
  void findById_returnsNullForUnknownId() {
    EventLog result = repository.findById(Long.MAX_VALUE).await().indefinitely();
    assertThat(result).isNull();
  }

  @Test
  void findSchedulingEvents_returnsEmptyWhenNoneMatch() {
    List<EventLog> result =
        repository.findSchedulingEvents(UUID.randomUUID(), "ghost").await().indefinitely();
    assertThat(result).isEmpty();
  }

  @Test
  void findByTypes_returnsEmptyWhenNoneMatch() {
    List<EventLog> result =
        repository.findByTypes(List.of(CaseHubEventType.CASE_CANCELLED)).await().indefinitely();
    assertThat(result).isEmpty();
  }

  @Test
  void idsAreUnique() {
    EventLog a = event(UUID.randomUUID(), "w", CaseHubEventType.WORKER_SCHEDULED);
    EventLog b = event(UUID.randomUUID(), "w", CaseHubEventType.WORKER_SCHEDULED);

    repository.append(a).await().indefinitely();
    repository.append(b).await().indefinitely();

    assertThat(a.id).isNotEqualTo(b.id);
    assertThat(a.getSeq()).isNotEqualTo(b.getSeq());
  }

  // --- Helper ---

  private EventLog event(UUID caseId, String workerId, CaseHubEventType type) {
    EventLog log = new EventLog();
    log.setCaseId(caseId);
    log.setWorkerId(workerId);
    log.setEventType(type);
    log.setStreamType(EventStreamType.WORKER);
    log.setTimestamp(Instant.now());
    return log;
  }
}
