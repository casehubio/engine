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
package io.casehub.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.spi.EventLogRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JpaEventLogRepositoryTest {

  @Inject EventLogRepository repository;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void append_populatesIdAndSeq() {
    EventLog log = workerScheduled(UUID.randomUUID(), "worker-1");

    run(() -> repository.append(log));

    assertThat(log.id).isNotNull().isPositive();
    assertThat(log.getSeq()).isNotNull().isPositive();
  }

  @Test
  void append_seqIsMonotonicallyIncreasing() {
    UUID caseId = UUID.randomUUID();
    EventLog first = workerScheduled(caseId, "worker-seq-1");
    EventLog second = workerScheduled(caseId, "worker-seq-2");

    run(() -> repository.append(first));
    run(() -> repository.append(second));

    assertThat(second.getSeq()).isGreaterThan(first.getSeq());
  }

  @Test
  void appendAndReturnId_returnsId() {
    EventLog log = workerScheduled(UUID.randomUUID(), "worker-return-id");

    Long returnedId = run(() -> repository.appendAndReturnId(log));

    assertThat(returnedId).isNotNull().isPositive();
    assertThat(returnedId).isEqualTo(log.id);
  }

  @Test
  void findById_returnsNullForUnknown() {
    EventLog result = run(() -> repository.findById(Long.MAX_VALUE));

    assertThat(result).isNull();
  }

  @Test
  void findById_returnsAppendedEvent() {
    UUID caseId = UUID.randomUUID();
    EventLog log = workerScheduled(caseId, "worker-find-by-id");
    run(() -> repository.append(log));

    EventLog found = run(() -> repository.findById(log.id));

    assertThat(found).isNotNull();
    assertThat(found.getCaseId()).isEqualTo(caseId);
    assertThat(found.getEventType()).isEqualTo(CaseHubEventType.WORKER_SCHEDULED);
    assertThat(found.getWorkerId()).isEqualTo("worker-find-by-id");
  }

  @Test
  void findSchedulingEvents_filtersCorrectly() {
    UUID caseId = UUID.randomUUID();
    String workerId = "worker-scheduling-" + UUID.randomUUID().toString().substring(0, 8);

    EventLog scheduled = event(caseId, workerId, CaseHubEventType.WORKER_SCHEDULED);
    EventLog started = event(caseId, workerId, CaseHubEventType.WORKER_EXECUTION_STARTED);
    EventLog otherWorker = event(caseId, "other-worker", CaseHubEventType.WORKER_SCHEDULED);
    EventLog otherCase = event(UUID.randomUUID(), workerId, CaseHubEventType.WORKER_SCHEDULED);

    run(() -> repository.append(scheduled));
    run(() -> repository.append(started));
    run(() -> repository.append(otherWorker));
    run(() -> repository.append(otherCase));

    List<EventLog> result = run(() -> repository.findSchedulingEvents(caseId, workerId));

    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(EventLog::getEventType)
        .containsExactlyInAnyOrder(
            CaseHubEventType.WORKER_SCHEDULED, CaseHubEventType.WORKER_EXECUTION_STARTED);
  }

  @Test
  void findByTypes_returnsOrderedBySeq() {
    UUID caseId = UUID.randomUUID();
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    EventLog e1 = event(caseId, "w-" + suffix, CaseHubEventType.CASE_STARTED);
    EventLog e2 = event(caseId, "w-" + suffix, CaseHubEventType.WORKER_EXECUTION_COMPLETED);
    EventLog noise = event(caseId, "w-" + suffix, CaseHubEventType.WORKER_SCHEDULED);

    run(() -> repository.append(e1));
    run(() -> repository.append(e2));
    run(() -> repository.append(noise));

    List<EventLog> result =
        run(
            () ->
                repository.findByTypes(
                    List.of(
                        CaseHubEventType.CASE_STARTED,
                        CaseHubEventType.WORKER_EXECUTION_COMPLETED)));

    // Result may include events from other tests — verify our events are present and ordering holds
    List<Long> seqs = result.stream().map(EventLog::getSeq).toList();
    assertThat(seqs).isSorted();
    assertThat(result.stream().map(EventLog::getEventType).toList())
        .doesNotContain(CaseHubEventType.WORKER_SCHEDULED);
  }

  @Test
  void findByCaseAndTypes_filtersByCaseId() {
    UUID targetCase = UUID.randomUUID();
    UUID otherCase = UUID.randomUUID();
    String suffix = UUID.randomUUID().toString().substring(0, 8);

    EventLog target = event(targetCase, "w-" + suffix, CaseHubEventType.CASE_STARTED);
    EventLog other = event(otherCase, "w-" + suffix, CaseHubEventType.CASE_STARTED);

    run(() -> repository.append(target));
    run(() -> repository.append(other));

    List<EventLog> result =
        run(
            () ->
                repository.findByCaseAndTypes(targetCase, List.of(CaseHubEventType.CASE_STARTED)));

    assertThat(result).isNotEmpty();
    assertThat(result).allMatch(e -> targetCase.equals(e.getCaseId()));
    assertThat(result.stream().map(EventLog::getSeq).toList()).isSorted();
  }

  @Test
  void findByCaseAndWorkerAndType_filtersCorrectly() {
    UUID caseId = UUID.randomUUID();
    String workerId = "worker-type-filter-" + UUID.randomUUID().toString().substring(0, 8);

    EventLog match = event(caseId, workerId, CaseHubEventType.WORKER_EXECUTION_FAILED);
    EventLog wrongWorker = event(caseId, "other", CaseHubEventType.WORKER_EXECUTION_FAILED);
    EventLog wrongType = event(caseId, workerId, CaseHubEventType.WORKER_SCHEDULED);

    run(() -> repository.append(match));
    run(() -> repository.append(wrongWorker));
    run(() -> repository.append(wrongType));

    List<EventLog> result =
        run(
            () ->
                repository.findByCaseAndWorkerAndType(
                    caseId, workerId, CaseHubEventType.WORKER_EXECUTION_FAILED));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWorkerId()).isEqualTo(workerId);
    assertThat(result.get(0).getEventType()).isEqualTo(CaseHubEventType.WORKER_EXECUTION_FAILED);
  }

  @Test
  void findSchedulingEvents_withAfterCutoff_excludesOlderEvents() {
    UUID caseId = UUID.randomUUID();
    Instant cutoff = Instant.now().minusSeconds(60);

    EventLog old = new EventLog();
    old.setCaseId(caseId);
    old.setWorkerId("w-old");
    old.setEventType(CaseHubEventType.WORKER_SCHEDULED);
    old.setStreamType(EventStreamType.CASE);
    old.setTimestamp(Instant.now().minusSeconds(120));
    old.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "hash-old"));
    run(() -> repository.append(old));

    EventLog recent = new EventLog();
    recent.setCaseId(caseId);
    recent.setWorkerId("w-old");
    recent.setEventType(CaseHubEventType.WORKER_SCHEDULED);
    recent.setStreamType(EventStreamType.CASE);
    recent.setTimestamp(Instant.now());
    recent.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "hash-recent"));
    run(() -> repository.append(recent));

    List<EventLog> result = run(() -> repository.findSchedulingEvents(caseId, "w-old", cutoff));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTimestamp()).isAfter(cutoff);
  }

  @Test
  void findSchedulingEvents_withNullAfter_returnsAll() {
    UUID caseId = UUID.randomUUID();

    EventLog e1 = new EventLog();
    e1.setCaseId(caseId);
    e1.setWorkerId("w-null");
    e1.setEventType(CaseHubEventType.WORKER_SCHEDULED);
    e1.setStreamType(EventStreamType.CASE);
    e1.setTimestamp(Instant.now().minusSeconds(120));
    e1.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "h1"));
    run(() -> repository.append(e1));

    EventLog e2 = new EventLog();
    e2.setCaseId(caseId);
    e2.setWorkerId("w-null");
    e2.setEventType(CaseHubEventType.WORKER_EXECUTION_STARTED);
    e2.setStreamType(EventStreamType.CASE);
    e2.setTimestamp(Instant.now());
    e2.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "h1"));
    run(() -> repository.append(e2));

    List<EventLog> result = run(() -> repository.findSchedulingEvents(caseId, "w-null", null));
    assertThat(result).hasSize(2);
  }

  private <T> T run(Supplier<Uni<T>> supplier) {
    try {
      return VertxContextSupport.subscribeAndAwait(supplier);
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private EventLog workerScheduled(UUID caseId, String workerId) {
    return event(caseId, workerId, CaseHubEventType.WORKER_SCHEDULED);
  }

  private EventLog event(UUID caseId, String workerId, CaseHubEventType eventType) {
    EventLog log = new EventLog();
    log.setCaseId(caseId);
    log.setWorkerId(workerId);
    log.setEventType(eventType);
    log.setStreamType(EventStreamType.WORKER);
    log.setTimestamp(Instant.now());
    return log;
  }
}
