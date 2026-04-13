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
package io.casehub.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.engine.handler.WorkerScheduleEventHandler;
import io.casehub.engine.internal.event.WorkerScheduleEvent;
import io.casehub.engine.internal.history.CaseHubEventType;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.history.EventStreamType;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.util.ReactiveUtils;
import io.casehub.engine.internal.util.WorkerExecutionKeys;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WorkerScheduleDedupTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject DedupCaseHubBean bean;

  @Inject WorkerScheduleEventHandler handler;

  @Inject CaseInstanceCache caseInstanceCache;

  @Inject Mutiny.SessionFactory sessionFactory;

  @Inject Vertx vertx;

  @Test
  void shouldSkipWhenExecutionAlreadyCompleted() {
    DedupCaseHubBean.runCount.set(0);

    UUID caseId =
        bean.startCase(Map.of("documentId", "doc-completed", "status", "queued"))
            .toCompletableFuture()
            .join();

    CaseInstance instance = caseInstanceCache.get(caseId);
    assertNotNull(instance);

    String executionIdempotency =
        WorkerExecutionKeys.inputDataHash(
            "dedup-worker",
            "dedupCapability",
            Map.of("documentId", "doc-completed", "status", "queued"));
    persistEvent(
        eventLog(
            caseId,
            CaseHubEventType.WORKER_EXECUTION_COMPLETED,
            "dedup-worker",
            executionIdempotency,
            Map.of("status", "processed")));

    ReactiveUtils.runOnSafeVertxContext(
            vertx,
            () ->
                handler.onWorkerScheduleEventHandler(
                    new WorkerScheduleEvent(instance, bean.worker(), bean.capability())))
        .await()
        .atMost(Duration.ofSeconds(10));

    Awaitility.await()
        .during(2, TimeUnit.SECONDS)
        .atMost(3, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertEquals(0, DedupCaseHubBean.runCount.get());
              assertEquals(
                  0,
                  countEvents(
                      caseId,
                      CaseHubEventType.WORKER_SCHEDULED,
                      "dedup-worker",
                      executionIdempotency));
            });
  }

  @Test
  void shouldResubmitExistingScheduledWithoutCreatingDuplicateLog() {
    DedupCaseHubBean.runCount.set(0);

    UUID caseId =
        bean.startCase(Map.of("documentId", "doc-resubmit", "status", "queued"))
            .toCompletableFuture()
            .join();

    CaseInstance instance = caseInstanceCache.get(caseId);
    assertNotNull(instance);

    String executionIdempotency =
        WorkerExecutionKeys.inputDataHash(
            "dedup-worker",
            "dedupCapability",
            Map.of("documentId", "doc-resubmit", "status", "queued"));
    persistEvent(
        eventLog(
            caseId,
            CaseHubEventType.WORKER_SCHEDULED,
            "dedup-worker",
            executionIdempotency,
            Map.of("documentId", "doc-resubmit", "status", "queued")));

    ReactiveUtils.runOnSafeVertxContext(
            vertx,
            () ->
                handler.onWorkerScheduleEventHandler(
                    new WorkerScheduleEvent(instance, bean.worker(), bean.capability())))
        .await()
        .atMost(Duration.ofSeconds(10));

    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertEquals(1, DedupCaseHubBean.runCount.get());
              assertEquals(
                  1,
                  countEvents(
                      caseId,
                      CaseHubEventType.WORKER_SCHEDULED,
                      "dedup-worker",
                      executionIdempotency));
              assertEquals(
                  "processed",
                  bean.query(caseId, "status", String.class).toCompletableFuture().join());
            });
  }

  private void persistEvent(EventLog eventLog) {
    ReactiveUtils.runOnSafeVertxContext(
            vertx, () -> sessionFactory.withTransaction(session -> session.persist(eventLog)))
        .await()
        .atMost(Duration.ofSeconds(10));
  }

  private long countEvents(
      UUID caseId, CaseHubEventType eventType, String workerId, String inputDataHash) {
    List<EventLog> eventLogs =
        ReactiveUtils.runOnSafeVertxContext(
                vertx,
                () ->
                    sessionFactory.withSession(
                        session ->
                            session
                                .createSelectionQuery(
                                    "from EventLog where caseId = :caseId and eventType = :eventType and workerId = :workerId",
                                    EventLog.class)
                                .setParameter("caseId", caseId)
                                .setParameter("eventType", eventType)
                                .setParameter("workerId", workerId)
                                .getResultList()))
            .await()
            .atMost(Duration.ofSeconds(10));

    return eventLogs.stream()
        .filter(
            eventLog -> {
              JsonNode metadata = eventLog.getMetadata();
              JsonNode existingHash = metadata == null ? null : metadata.get("inputDataHash");
              return existingHash != null && inputDataHash.equals(existingHash.asText());
            })
        .count();
  }

  private EventLog eventLog(
      UUID caseId,
      CaseHubEventType eventType,
      String workerId,
      String inputDataHash,
      Map<String, Object> payload) {
    EventLog eventLog = new EventLog();
    eventLog.setCaseId(caseId);
    eventLog.setEventType(eventType);
    eventLog.setStreamType(EventStreamType.CASE);
    eventLog.setTimestamp(Instant.now());
    eventLog.setWorkerId(workerId);
    eventLog.setMetadata(
        OBJECT_MAPPER.valueToTree(
            Map.of(
                "workerName", workerId,
                "capabilityName", "dedupCapability",
                "inputDataHash", inputDataHash)));
    eventLog.setPayload(OBJECT_MAPPER.valueToTree(payload));
    return eventLog;
  }

  @ApplicationScoped
  public static class DedupCaseHubBean extends CaseHub {

    static final AtomicInteger runCount = new AtomicInteger();

    private final Capability capability =
        Capability.builder()
            .name("dedupCapability")
            .inputSchema("{ documentId: .documentId, status: .status }")
            .outputSchema("{ status: .status, processedDocument: .processedDocument }")
            .build();

    private final Worker worker =
        Worker.builder()
            .name("dedup-worker")
            .capabilities(capability)
            .function(
                input -> {
                  runCount.incrementAndGet();
                  return Map.of(
                      "status",
                      "processed",
                      "processedDocument",
                      Map.of("id", input.get("documentId")));
                })
            .build();

    @Override
    public CaseHubDefinition getDefinition() {
      return CaseHubDefinition.builder()
          .namespace("test-worker-schedule-dedup")
          .name("Worker Schedule Dedup Test")
          .version("1.0.0")
          .capabilities(capability)
          .workers(worker)
          .build();
    }

    Capability capability() {
      return capability;
    }

    Worker worker() {
      return worker;
    }
  }
}
