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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.TaskStatus;
import io.casehub.api.view.CaseStreamEventView;
import io.casehub.engine.common.spi.event.CaseContextUpdatedEvent;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseStreamSpringBroadcasterTest {

  private CaseStreamSpringBroadcaster broadcaster;

  @BeforeEach
  void setUp() {
    broadcaster = new CaseStreamSpringBroadcaster();
  }

  @Test
  void planItemEventProducesStreamEvent() throws Exception {
    UUID caseId = UUID.randomUUID();
    var collector = new EventCollector<CaseStreamEventView>(1);
    broadcaster.stream(caseId).subscribe(collector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            caseId, "pi-1", "analysis", TaskStatus.PENDING, TaskStatus.RUNNING, "t1"));

    assertTrue(collector.await());
    assertEquals(1, collector.items.size());
    assertEquals(caseId, collector.items.get(0).caseId());
    assertEquals("plan-item", collector.items.get(0).type());
    assertEquals("pi-1", collector.items.get(0).data().get("planItemId"));
  }

  @Test
  void contextEventProducesStreamEvent() throws Exception {
    UUID caseId = UUID.randomUUID();
    var collector = new EventCollector<CaseStreamEventView>(1);
    broadcaster.stream(caseId).subscribe(collector);

    broadcaster.onContextUpdated(new CaseContextUpdatedEvent(caseId, "working", "t1"));

    assertTrue(collector.await());
    assertEquals(1, collector.items.size());
    assertEquals("context", collector.items.get(0).type());
    assertEquals("working", collector.items.get(0).data().get("changedLayer"));
  }

  @Test
  void filtersByCaseId() throws Exception {
    UUID targetCase = UUID.randomUUID();
    UUID otherCase = UUID.randomUUID();

    var targetCollector = new EventCollector<CaseStreamEventView>(1);
    broadcaster.stream(targetCase).subscribe(targetCollector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            otherCase, "pi-other", "review", TaskStatus.PENDING, TaskStatus.RUNNING, "t1"));

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            targetCase, "pi-1", "analysis", TaskStatus.RUNNING, TaskStatus.COMPLETED, "t1"));

    assertTrue(targetCollector.await());
    assertEquals(1, targetCollector.items.size());
    assertEquals(targetCase, targetCollector.items.get(0).caseId());
  }

  @Test
  void cleanupRemovesDeadSubscribers() throws Exception {
    UUID caseId = UUID.randomUUID();
    var collector = new EventCollector<CaseStreamEventView>(1);
    var publisher = broadcaster.stream(caseId);
    publisher.subscribe(collector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            caseId, "pi-1", "analysis", TaskStatus.PENDING, TaskStatus.RUNNING, "t1"));

    assertTrue(collector.await());
    collector.subscription.cancel();

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(
            caseId, "pi-2", "review", TaskStatus.PENDING, TaskStatus.RUNNING, "t1"));
  }

  @Test
  void previousStatusNullHandledAsNone() throws Exception {
    UUID caseId = UUID.randomUUID();
    var collector = new EventCollector<CaseStreamEventView>(1);
    broadcaster.stream(caseId).subscribe(collector);

    broadcaster.onPlanItemChanged(
        new PlanItemStateChangedEvent(caseId, "pi-1", "analysis", null, TaskStatus.PENDING, "t1"));

    assertTrue(collector.await());
    assertEquals("NONE", collector.items.get(0).data().get("previousStatus"));
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
