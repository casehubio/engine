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
package io.casehub.engine.runtime.spring.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.event.CaseContextUpdatedEvent;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.plan.execution.CasePlanModelSnapshotProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecutionStateSpringBroadcasterTest {

  @Mock CasePlanModelSnapshotProvider planModelProvider;
  @Mock ExecutionSnapshotStore snapshotStore;
  @Mock CaseDefinitionRegistry definitionRegistry;
  @Mock CaseInstanceRepository caseInstanceRepository;

  private ExecutionStateSpringBroadcaster broadcaster;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeEach
  void setUp() {
    broadcaster =
        new ExecutionStateSpringBroadcaster(
            planModelProvider,
            snapshotStore,
            definitionRegistry,
            caseInstanceRepository,
            objectMapper);
  }

  @Test
  void planItemEventProducesJsonSnapshot() throws Exception {
    UUID caseId = UUID.randomUUID();
    String tenancyId = "t1";

    when(planModelProvider.getSnapshot(caseId, tenancyId)).thenReturn(Optional.empty());
    when(snapshotStore.getDagPlan(caseId, tenancyId)).thenReturn(Optional.empty());
    when(snapshotStore.getDagResult(caseId, tenancyId)).thenReturn(Optional.empty());
    when(caseInstanceRepository.findByUuid(caseId, tenancyId)).thenReturn(Optional.empty());

    var collector = new EventCollector<JsonNode>(1);
    broadcaster.stream(caseId).subscribe(collector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            caseId, "pi-1", "analysis", TaskStatus.PENDING, TaskStatus.RUNNING, tenancyId));

    assertTrue(collector.await());
    assertEquals(1, collector.items.size());
    JsonNode json = collector.items.get(0);
    assertEquals(caseId.toString(), json.get("executionId").asText());
    assertEquals("IDLE", json.get("state").asText());
  }

  @Test
  void contextEventProducesJsonSnapshot() throws Exception {
    UUID caseId = UUID.randomUUID();
    String tenancyId = "t1";

    when(planModelProvider.getSnapshot(caseId, tenancyId)).thenReturn(Optional.empty());
    when(snapshotStore.getDagPlan(caseId, tenancyId)).thenReturn(Optional.empty());
    when(snapshotStore.getDagResult(caseId, tenancyId)).thenReturn(Optional.empty());
    when(caseInstanceRepository.findByUuid(caseId, tenancyId)).thenReturn(Optional.empty());

    var collector = new EventCollector<JsonNode>(1);
    broadcaster.stream(caseId).subscribe(collector);

    broadcaster.onContextUpdated(new CaseContextUpdatedEvent(caseId, "working", tenancyId));

    assertTrue(collector.await());
    assertEquals(1, collector.items.size());
    assertNotNull(collector.items.get(0).get("executionId"));
  }

  @Test
  void filtersByCaseId() throws Exception {
    UUID targetCase = UUID.randomUUID();
    UUID otherCase = UUID.randomUUID();
    String tenancyId = "t1";

    when(planModelProvider.getSnapshot(any(), any())).thenReturn(Optional.empty());
    when(snapshotStore.getDagPlan(any(), any())).thenReturn(Optional.empty());
    when(snapshotStore.getDagResult(any(), any())).thenReturn(Optional.empty());
    when(caseInstanceRepository.findByUuid(any(), any())).thenReturn(Optional.empty());

    var targetCollector = new EventCollector<JsonNode>(1);
    broadcaster.stream(targetCase).subscribe(targetCollector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            otherCase, "pi-other", "review", TaskStatus.PENDING, TaskStatus.RUNNING, tenancyId));

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            targetCase, "pi-1", "analysis", TaskStatus.RUNNING, TaskStatus.COMPLETED, tenancyId));

    assertTrue(targetCollector.await());
    assertEquals(1, targetCollector.items.size());
    assertEquals(targetCase.toString(), targetCollector.items.get(0).get("executionId").asText());
  }

  @Test
  void composeInitialReturnsNullWhenNoData() {
    UUID caseId = UUID.randomUUID();
    String tenancyId = "t1";

    when(planModelProvider.getSnapshot(caseId, tenancyId)).thenReturn(Optional.empty());
    when(snapshotStore.getDagPlan(caseId, tenancyId)).thenReturn(Optional.empty());
    when(snapshotStore.getDagResult(caseId, tenancyId)).thenReturn(Optional.empty());

    assertNull(broadcaster.composeInitial(caseId, tenancyId));
  }

  @Test
  void compositionFailureIsSwallowed() throws Exception {
    UUID caseId = UUID.randomUUID();
    String tenancyId = "t1";

    when(planModelProvider.getSnapshot(caseId, tenancyId))
        .thenThrow(new RuntimeException("db down"));

    var collector = new EventCollector<JsonNode>(1);
    broadcaster.stream(caseId).subscribe(collector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            caseId, "pi-1", "analysis", TaskStatus.PENDING, TaskStatus.RUNNING, tenancyId));

    // Should not throw — failure is logged and swallowed
    Thread.sleep(200);
    assertEquals(0, collector.items.size());
  }

  private static class EventCollector<T> implements Flow.Subscriber<T> {
    final List<T> items = new ArrayList<>();
    final CountDownLatch latch;
    Flow.Subscription subscription;

    EventCollector(int expectedCount) {
      latch = new CountDownLatch(expectedCount);
    }

    @Override
    public void onSubscribe(Flow.Subscription s) {
      subscription = s;
      s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
      items.add(item);
      latch.countDown();
    }

    @Override
    public void onError(Throwable throwable) {}

    @Override
    public void onComplete() {}

    boolean await() throws InterruptedException {
      return latch.await(5, TimeUnit.SECONDS);
    }
  }
}
