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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.model.CaseChannel;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkResult;
import io.casehub.api.model.WorkerContext;
import io.casehub.api.spi.CaseChannelProvider;
import io.casehub.api.spi.WorkerContextProvider;
import io.casehub.api.spi.WorkerStatusListener;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that WorkerStatusListener, WorkerContextProvider, and CaseChannelProvider are called at
 * the correct lifecycle points by the engine. Refs casehubio/engine#152.
 */
@QuarkusTest
class SpiWiringIntegrationTest {

  @Inject SimpleCaseHubBean simpleCaseHubBean;
  @Inject CaseFaultedStateTest.AlwaysFailingCaseHubBean alwaysFailingBean;
  @Inject CaseInstanceCache caseInstanceCache;
  @Inject RecordingWorkerStatusListener statusListener;
  @Inject RecordingWorkerContextProvider contextProvider;
  @Inject RecordingCaseChannelProvider channelProvider;

  @BeforeEach
  void reset() {
    RecordingWorkerStatusListener.reset();
    RecordingWorkerContextProvider.reset();
    RecordingCaseChannelProvider.reset();
  }

  // ------------------------------------------------------------------ //
  // WorkerStatusListener                                                 //
  // ------------------------------------------------------------------ //

  @Test
  void onWorkerStartedCalledWhenWorkerBegins() {
    UUID caseId =
        simpleCaseHubBean
            .startCase(Map.of("documentId", "doc-1", "status", "processing"))
            .toCompletableFuture()
            .join();

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(RecordingWorkerStatusListener.startedWorkerIds)
                    .as("onWorkerStarted must be called when a worker begins execution")
                    .isNotEmpty());
  }

  @Test
  void onWorkerCompletedCalledAfterSuccessfulExecution() {
    UUID caseId =
        simpleCaseHubBean
            .startCase(Map.of("documentId", "doc-2", "status", "processing"))
            .toCompletableFuture()
            .join();

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(RecordingWorkerStatusListener.completedWorkerIds)
                    .as("onWorkerCompleted must be called after worker finishes successfully")
                    .isNotEmpty());

    assertThat(RecordingWorkerStatusListener.lastCompletedResult)
        .as("completed result must carry the output and workerId")
        .isNotNull();
    assertThat(RecordingWorkerStatusListener.lastCompletedResult.status().name())
        .isEqualTo("COMPLETED");
  }

  @Test
  void onWorkerStalledCalledWhenRetriesExhausted() {
    CaseFaultedStateTest.AlwaysFailingCaseHubBean.runCount.set(0);
    UUID caseId =
        alwaysFailingBean.startCase(Map.of("status", "processing")).toCompletableFuture().join();

    await()
        .atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(RecordingWorkerStatusListener.stalledWorkerIds)
                    .as("onWorkerStalled must be called when all retries are exhausted")
                    .isNotEmpty());
  }

  // ------------------------------------------------------------------ //
  // WorkerContextProvider                                                //
  // ------------------------------------------------------------------ //

  @Test
  void buildContextCalledBeforeWorkerScheduling() {
    UUID caseId =
        simpleCaseHubBean
            .startCase(Map.of("documentId", "doc-3", "status", "processing"))
            .toCompletableFuture()
            .join();

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(RecordingWorkerContextProvider.buildContextCallCount.get())
                    .as("buildContext must be called at least once when a worker is scheduled")
                    .isGreaterThan(0));
  }

  @Test
  void buildContextReceivesCorrectCapabilityName() {
    UUID caseId =
        simpleCaseHubBean
            .startCase(Map.of("documentId", "doc-4", "status", "processing"))
            .toCompletableFuture()
            .join();

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(RecordingWorkerContextProvider.seenCapabilities)
                    .as("buildContext must receive the binding's capability name")
                    .contains("processDocument"));
  }

  // ------------------------------------------------------------------ //
  // CaseChannelProvider                                                  //
  // ------------------------------------------------------------------ //

  @Test
  void openChannelCalledWhenCaseStarts() {
    UUID caseId =
        simpleCaseHubBean
            .startCase(Map.of("documentId", "doc-5", "status", "processing"))
            .toCompletableFuture()
            .join();

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(RecordingCaseChannelProvider.openedCaseIds)
                    .as("openChannel must be called when a case starts")
                    .contains(caseId));
  }

  @Test
  void closeChannelCalledWhenCaseReachesTerminalState() {
    UUID caseId =
        simpleCaseHubBean
            .startCase(Map.of("documentId", "doc-6", "status", "processing"))
            .toCompletableFuture()
            .join();

    await()
        .atMost(15, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(caseInstanceCache.get(caseId).getState())
                    .isEqualTo(CaseStatus.COMPLETED));

    assertThat(RecordingCaseChannelProvider.closedCaseIds)
        .as("closeChannel must be called when a case reaches a terminal state")
        .contains(caseId);
  }

  // ------------------------------------------------------------------ //
  // Recording SPI implementations                                        //
  // ------------------------------------------------------------------ //

  @Alternative
  @Priority(1)
  @ApplicationScoped
  public static class RecordingWorkerStatusListener implements WorkerStatusListener {

    static final List<String> startedWorkerIds = new CopyOnWriteArrayList<>();
    static final List<String> completedWorkerIds = new CopyOnWriteArrayList<>();
    static final List<String> stalledWorkerIds = new CopyOnWriteArrayList<>();
    static volatile WorkResult lastCompletedResult;

    static void reset() {
      startedWorkerIds.clear();
      completedWorkerIds.clear();
      stalledWorkerIds.clear();
      lastCompletedResult = null;
    }

    @Override
    public void onWorkerStarted(String workerId, Map<String, String> sessionMeta) {
      startedWorkerIds.add(workerId);
    }

    @Override
    public void onWorkerCompleted(String workerId, WorkResult result) {
      completedWorkerIds.add(workerId);
      lastCompletedResult = result;
    }

    @Override
    public void onWorkerStalled(String workerId) {
      stalledWorkerIds.add(workerId);
    }
  }

  @Alternative
  @Priority(1)
  @ApplicationScoped
  public static class RecordingWorkerContextProvider implements WorkerContextProvider {

    static final AtomicInteger buildContextCallCount = new AtomicInteger(0);
    static final Set<String> seenCapabilities = ConcurrentHashMap.newKeySet();

    static void reset() {
      buildContextCallCount.set(0);
      seenCapabilities.clear();
    }

    @Override
    public WorkerContext buildContext(String workerId, WorkRequest task) {
      buildContextCallCount.incrementAndGet();
      seenCapabilities.add(task.capability());
      return new WorkerContext(task.capability(), null, null, List.of(), null, Map.of());
    }
  }

  @Alternative
  @Priority(1)
  @ApplicationScoped
  public static class RecordingCaseChannelProvider implements CaseChannelProvider {

    static final Set<UUID> openedCaseIds = ConcurrentHashMap.newKeySet();
    static final Set<UUID> closedCaseIds = ConcurrentHashMap.newKeySet();
    // NOT thread-safe — only used within a single case's lifecycle
    private final Map<UUID, List<CaseChannel>> openChannels = new ConcurrentHashMap<>();

    static void reset() {
      openedCaseIds.clear();
      closedCaseIds.clear();
    }

    @Override
    public CaseChannel openChannel(UUID caseId, String purpose) {
      openedCaseIds.add(caseId);
      CaseChannel channel =
          new CaseChannel(caseId + "/" + purpose, purpose, purpose, "none", Map.of());
      openChannels.computeIfAbsent(caseId, id -> new CopyOnWriteArrayList<>()).add(channel);
      return channel;
    }

    @Override
    public void postToChannel(CaseChannel channel, String from, String content) {}

    @Override
    public void closeChannel(CaseChannel channel) {
      // channel id is caseId/purpose — extract caseId prefix
      String id = channel.id();
      int slash = id.indexOf('/');
      if (slash > 0) {
        try {
          closedCaseIds.add(UUID.fromString(id.substring(0, slash)));
        } catch (IllegalArgumentException ignored) {
        }
      }
    }

    @Override
    public List<CaseChannel> listChannels(UUID caseId) {
      return openChannels.getOrDefault(caseId, List.of());
    }
  }
}
