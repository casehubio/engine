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
package io.casehub.engine.common.spi;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.event.EventStreamType;
import io.casehub.engine.common.internal.history.EventLog;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public abstract class CrossTenantEventLogRepositoryContractTest {

  protected abstract CrossTenantEventLogRepository repository();

  protected abstract void appendEvent(EventLog event, String tenancyId);

  private final UUID caseId = UUID.randomUUID();

  protected EventLog newEvent(
      UUID caseId, CaseHubEventType type, String workerId, String tenancyId) {
    EventLog log = new EventLog();
    log.setCaseId(caseId);
    log.setEventType(type);
    log.setStreamType(EventStreamType.CASE);
    log.setTimestamp(Instant.now());
    log.setWorkerId(workerId);
    log.tenancyId = tenancyId;
    return log;
  }

  @Test
  void findByTypes_returnsMatchingEvents() {
    EventLog e1 = newEvent(caseId, CaseHubEventType.CASE_STARTED, null, "t1");
    appendEvent(e1, "t1");
    EventLog e2 = newEvent(caseId, CaseHubEventType.SIGNAL_RECEIVED, null, "t2");
    appendEvent(e2, "t2");

    Collection<CaseHubEventType> types = Set.of(CaseHubEventType.CASE_STARTED);
    assertThat(repository().findByTypes(types)).hasSize(1);
    assertThat(repository().findByTypes(types).get(0).getEventType())
        .isEqualTo(CaseHubEventType.CASE_STARTED);
  }

  @Test
  void findByCaseAndTypes_filtersCorrectly() {
    UUID other = UUID.randomUUID();
    EventLog e1 = newEvent(caseId, CaseHubEventType.CASE_STARTED, null, "t1");
    appendEvent(e1, "t1");
    EventLog e2 = newEvent(other, CaseHubEventType.CASE_STARTED, null, "t1");
    appendEvent(e2, "t1");

    var result = repository().findByCaseAndTypes(caseId, Set.of(CaseHubEventType.CASE_STARTED));
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCaseId()).isEqualTo(caseId);
  }

  @Test
  void findById_returnsStoredEvent() {
    EventLog event = newEvent(caseId, CaseHubEventType.CASE_STARTED, null, "t1");
    appendEvent(event, "t1");

    assertThat(event.id).isNotNull();
    assertThat(repository().findById(event.id))
        .isPresent()
        .get()
        .extracting(EventLog::getCaseId)
        .isEqualTo(caseId);
  }

  @Test
  void findById_returnsEmptyForUnknownId() {
    assertThat(repository().findById(-999L)).isEmpty();
  }

  @Test
  void findByWorkerAndTypeAcrossTenants_filtersCorrectly() {
    EventLog e1 = newEvent(caseId, CaseHubEventType.WORKER_EXECUTION_STARTED, "w1", "t1");
    appendEvent(e1, "t1");
    EventLog e2 = newEvent(caseId, CaseHubEventType.WORKER_EXECUTION_STARTED, "w2", "t2");
    appendEvent(e2, "t2");

    var result =
        repository()
            .findByWorkerAndTypeAcrossTenants("w1", CaseHubEventType.WORKER_EXECUTION_STARTED);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWorkerId()).isEqualTo("w1");
  }

  @Test
  void findByCaseAndWorkerAndType_filtersCorrectly() {
    EventLog e1 = newEvent(caseId, CaseHubEventType.WORKER_EXECUTION_STARTED, "w1", "t1");
    appendEvent(e1, "t1");
    EventLog e2 = newEvent(caseId, CaseHubEventType.WORKER_EXECUTION_STARTED, "w2", "t1");
    appendEvent(e2, "t1");

    var result =
        repository()
            .findByCaseAndWorkerAndType(caseId, "w1", CaseHubEventType.WORKER_EXECUTION_STARTED);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWorkerId()).isEqualTo("w1");
  }

  @Test
  void findByTypes_returnsEmptyWhenNoMatch() {
    assertThat(repository().findByTypes(Set.of(CaseHubEventType.CASE_STARTED))).isEmpty();
  }
}
