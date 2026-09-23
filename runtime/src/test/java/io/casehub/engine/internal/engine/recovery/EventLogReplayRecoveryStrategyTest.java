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
package io.casehub.engine.internal.engine.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.context.CaseContext;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CrossTenantEventLogRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventLogReplayRecoveryStrategyTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private StubEventLogRepository eventLogRepo;
  private EventLogReplayRecoveryStrategy strategy;

  @BeforeEach
  void setUp() {
    eventLogRepo = new StubEventLogRepository();
    strategy = new EventLogReplayRecoveryStrategy(eventLogRepo);
  }

  @Test
  void recoverFromCaseStartedEvent() {
    UUID caseId = UUID.randomUUID();
    ObjectNode layerDoc = MAPPER.createObjectNode();
    layerDoc.putObject("working").put("status", "active");

    eventLogRepo.events = List.of(eventLog(caseId, CaseHubEventType.CASE_STARTED, layerDoc, null));

    CaseInstance instance = new CaseInstance();
    instance.setUuid(caseId);

    CaseContext ctx = strategy.recover(instance);
    assertEquals("active", ctx.getString("status"));
  }

  @Test
  void recoverAppliesWorkerCompletionContextChanges() {
    UUID caseId = UUID.randomUUID();
    ObjectNode startPayload = MAPPER.createObjectNode();
    startPayload.putObject("working").put("count", 0);

    ObjectNode metadata = MAPPER.createObjectNode();
    ObjectNode changes = metadata.putObject("contextChanges");
    ObjectNode countChange = changes.putObject("count");
    countChange.put("before", 0);
    countChange.put("after", 5);

    eventLogRepo.events =
        List.of(
            eventLog(caseId, CaseHubEventType.CASE_STARTED, startPayload, null),
            eventLog(caseId, CaseHubEventType.WORKER_EXECUTION_COMPLETED, null, metadata));

    CaseInstance instance = new CaseInstance();
    instance.setUuid(caseId);

    CaseContext ctx = strategy.recover(instance);
    assertEquals(5, ctx.getInt("count"));
  }

  @Test
  void recoverAppliesSubcaseCompletedPayload() {
    UUID caseId = UUID.randomUUID();
    ObjectNode startPayload = MAPPER.createObjectNode();
    startPayload.putObject("working").put("status", "initial");

    ObjectNode subcasePayload = MAPPER.createObjectNode();
    subcasePayload.put("result", "sub-done");

    eventLogRepo.events =
        List.of(
            eventLog(caseId, CaseHubEventType.CASE_STARTED, startPayload, null),
            eventLog(caseId, CaseHubEventType.SUBCASE_COMPLETED, subcasePayload, null));

    CaseInstance instance = new CaseInstance();
    instance.setUuid(caseId);

    CaseContext ctx = strategy.recover(instance);
    assertEquals("sub-done", ctx.getString("result"));
    assertEquals("initial", ctx.getString("status"));
  }

  @Test
  void recoverReplaysScopedWorkerOutputContextChanges() {
    UUID caseId = UUID.randomUUID();
    ObjectNode startPayload = MAPPER.createObjectNode();
    startPayload.putObject("working").put("status", "running");

    ObjectNode scopedMeta = MAPPER.createObjectNode();
    ObjectNode scopedChanges = scopedMeta.putObject("contextChanges");
    ObjectNode progressChange = scopedChanges.putObject("progress");
    progressChange.put("before", (String) null);
    progressChange.put("after", "50%");

    EventLog scopedEvent =
        eventLog(caseId, CaseHubEventType.SCOPED_WORKER_OUTPUT, null, scopedMeta);
    scopedEvent.setWorkerId("persistent-worker-1");

    eventLogRepo.events =
        List.of(eventLog(caseId, CaseHubEventType.CASE_STARTED, startPayload, null), scopedEvent);

    CaseInstance instance = new CaseInstance();
    instance.setUuid(caseId);

    CaseContext ctx = strategy.recover(instance);
    assertEquals("running", ctx.getString("status"));
    assertEquals("50%", ctx.getString("progress"));
  }

  @Test
  void onContextChangedIsNoOp() {
    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());

    strategy.onContextChanged(instance, null);

    assertNull(instance.getContextSnapshot());
  }

  private EventLog eventLog(
      UUID caseId,
      CaseHubEventType type,
      com.fasterxml.jackson.databind.JsonNode payload,
      com.fasterxml.jackson.databind.JsonNode metadata) {
    EventLog e = new EventLog();
    e.setCaseId(caseId);
    e.setEventType(type);
    e.setTimestamp(Instant.now());
    e.setPayload(payload);
    e.setMetadata(metadata);
    return e;
  }

  static class StubEventLogRepository implements CrossTenantEventLogRepository {
    List<EventLog> events = List.of();

    @Override
    public List<EventLog> findByCaseAndTypes(UUID caseId, Collection<CaseHubEventType> types) {
      return events.stream()
          .filter(e -> e.getCaseId().equals(caseId) && types.contains(e.getEventType()))
          .toList();
    }

    @Override
    public List<EventLog> findByTypes(Collection<CaseHubEventType> types) {
      return events.stream().filter(e -> types.contains(e.getEventType())).toList();
    }

    @Override
    public List<String> findSubmittedWorkWithoutCompletion() {
      return List.of();
    }

    @Override
    public List<EventLog> findByWorkerAndTypeAcrossTenants(String workerId, CaseHubEventType type) {
      return List.of();
    }

    @Override
    public EventLog findById(Long id) {
      return null;
    }

    @Override
    public List<EventLog> findByCaseAndWorkerAndType(
        UUID caseId, String workerId, CaseHubEventType type) {
      return List.of();
    }
  }
}
