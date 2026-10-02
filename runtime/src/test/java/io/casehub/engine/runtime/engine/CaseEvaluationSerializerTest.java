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
package io.casehub.engine.runtime.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CaseEvaluationSerializerTest {

  private final CaseEvaluationSerializer serializer =
      new CaseEvaluationSerializer(new QuiescenceTracker());

  @Test
  void runsEvaluatorImmediatelyWhenIdle() {
    AtomicInteger count = new AtomicInteger();
    serializer.submit(UUID.randomUUID(), count::incrementAndGet, null);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  void serialisesEvaluationsForSameCase() throws Exception {
    UUID caseId = UUID.randomUUID();
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch firstCanProceed = new CountDownLatch(1);
    AtomicInteger maxConcurrent = new AtomicInteger();
    AtomicInteger running = new AtomicInteger();
    CountDownLatch secondCompleted = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () ->
              serializer.submit(
                  caseId,
                  () -> {
                    int r = running.incrementAndGet();
                    maxConcurrent.updateAndGet(cur -> Math.max(cur, r));
                    firstStarted.countDown();
                    awaitQuietly(firstCanProceed);
                    running.decrementAndGet();
                  },
                  null));

      assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

      executor.submit(
          () -> {
            serializer.submit(
                caseId,
                () -> {
                  int r = running.incrementAndGet();
                  maxConcurrent.updateAndGet(cur -> Math.max(cur, r));
                  running.decrementAndGet();
                  secondCompleted.countDown();
                },
                null);
          });

      Thread.sleep(100);
      firstCanProceed.countDown();
      assertThat(secondCompleted.await(2, TimeUnit.SECONDS)).isTrue();

      assertThat(maxConcurrent.get()).isEqualTo(1);
    }
  }

  @Test
  void preservesAllPendingEventsInSubmissionOrder() throws Exception {
    UUID caseId = UUID.randomUUID();
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch firstCanProceed = new CountDownLatch(1);
    AtomicInteger totalEvaluations = new AtomicInteger();
    java.util.List<String> evaluated = new java.util.concurrent.CopyOnWriteArrayList<>();
    CountDownLatch allDone = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () ->
              serializer.submit(
                  caseId,
                  () -> {
                    totalEvaluations.incrementAndGet();
                    firstStarted.countDown();
                    awaitQuietly(firstCanProceed);
                  },
                  null));

      assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

      serializer.submit(
          caseId,
          () -> {
            totalEvaluations.incrementAndGet();
            evaluated.add("second");
          },
          null);

      serializer.submit(
          caseId,
          () -> {
            totalEvaluations.incrementAndGet();
            evaluated.add("third");
            allDone.countDown();
          },
          null);

      firstCanProceed.countDown();
      assertThat(allDone.await(2, TimeUnit.SECONDS)).isTrue();

      assertThat(totalEvaluations.get()).isEqualTo(3);
      assertThat(evaluated).containsExactly("second", "third");
    }
  }

  @Test
  void resetWaitsForActiveEvaluationBeforeAcceptingNewWork() throws Exception {
    UUID caseId = UUID.randomUUID();
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch firstCanProceed = new CountDownLatch(1);
    CountDownLatch resetStarted = new CountDownLatch(1);
    CountDownLatch secondCompleted = new CountDownLatch(1);
    AtomicInteger running = new AtomicInteger();
    AtomicInteger maxConcurrent = new AtomicInteger();
    AtomicBoolean resetCompleted = new AtomicBoolean();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () ->
              serializer.submit(
                  caseId,
                  () -> {
                    int active = running.incrementAndGet();
                    maxConcurrent.updateAndGet(current -> Math.max(current, active));
                    firstStarted.countDown();
                    awaitQuietly(firstCanProceed);
                    running.decrementAndGet();
                  },
                  null));

      assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

      CompletableFuture<Void> reset =
          CompletableFuture.runAsync(
              () -> {
                resetStarted.countDown();
                serializer.reset();
                resetCompleted.set(true);
              },
              executor);
      assertThat(resetStarted.await(2, TimeUnit.SECONDS)).isTrue();

      Thread.sleep(100);
      assertThat(resetCompleted).isFalse();

      CompletableFuture<Void> second =
          CompletableFuture.runAsync(
              () ->
                  serializer.submit(
                      caseId,
                      () -> {
                        int active = running.incrementAndGet();
                        maxConcurrent.updateAndGet(current -> Math.max(current, active));
                        running.decrementAndGet();
                        secondCompleted.countDown();
                      },
                      null),
              executor);

      firstCanProceed.countDown();
      reset.get(2, TimeUnit.SECONDS);
      second.get(2, TimeUnit.SECONDS);

      assertThat(secondCompleted.await(2, TimeUnit.SECONDS)).isTrue();
      assertThat(maxConcurrent).hasValue(1);
    }
  }

  @Test
  void allowsConcurrentEvaluationsForDifferentCases() throws Exception {
    UUID case1 = UUID.randomUUID();
    UUID case2 = UUID.randomUUID();
    CountDownLatch bothRunning = new CountDownLatch(2);
    CountDownLatch proceed = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () ->
              serializer.submit(
                  case1,
                  () -> {
                    bothRunning.countDown();
                    awaitQuietly(proceed);
                  },
                  null));
      executor.submit(
          () ->
              serializer.submit(
                  case2,
                  () -> {
                    bothRunning.countDown();
                    awaitQuietly(proceed);
                  },
                  null));

      assertThat(bothRunning.await(2, TimeUnit.SECONDS)).isTrue();
      proceed.countDown();
    }
  }

  @Test
  void evictCleansUpState() {
    UUID caseId = UUID.randomUUID();
    AtomicInteger count = new AtomicInteger();
    serializer.submit(caseId, count::incrementAndGet, null);
    serializer.evict(caseId);
    serializer.submit(caseId, count::incrementAndGet, null);
    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void queuedSubmissions_accumulateSignalIds() throws Exception {
    UUID caseId = UUID.randomUUID();
    UUID signal1 = UUID.randomUUID();
    UUID signal2 = UUID.randomUUID();
    UUID signal3 = UUID.randomUUID();
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch firstCanProceed = new CountDownLatch(1);
    Set<UUID> collectedSignals = ConcurrentHashMap.newKeySet();
    CountDownLatch allDone = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () -> {
            Set<UUID> signals =
                serializer.submit(
                    caseId,
                    () -> {
                      firstStarted.countDown();
                      awaitQuietly(firstCanProceed);
                    },
                    signal1);
            collectedSignals.addAll(signals);
          });

      assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

      // These two will be queued — both will execute after the first completes
      serializer.submit(caseId, () -> {}, signal2);
      serializer.submit(
          caseId,
          () -> {
            allDone.countDown();
          },
          signal3);

      firstCanProceed.countDown();
      assertThat(allDone.await(2, TimeUnit.SECONDS)).isTrue();

      // Wait for executor thread to finish collecting
      Thread.sleep(100);
    }

    // signal1 returned from first submit; signal2 + signal3 accumulated and returned from drain
    assertThat(collectedSignals).containsExactlyInAnyOrder(signal1, signal2, signal3);
  }

  @Test
  void reset_drainsInFlightEvaluations() throws Exception {
    UUID caseId = UUID.randomUUID();
    CountDownLatch evalStarted = new CountDownLatch(1);
    CountDownLatch evalCanProceed = new CountDownLatch(1);
    AtomicInteger evalCount = new AtomicInteger();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () ->
              serializer.submit(
                  caseId,
                  () -> {
                    evalCount.incrementAndGet();
                    evalStarted.countDown();
                    awaitQuietly(evalCanProceed);
                  },
                  null));

      assertThat(evalStarted.await(2, TimeUnit.SECONDS)).isTrue();

      // Start reset in background — should block until evaluation completes
      var resetFuture = executor.submit(() -> serializer.reset());

      // Let the evaluation finish — reset should then complete
      evalCanProceed.countDown();
      resetFuture.get(5, TimeUnit.SECONDS);

      // After reset, submissions work again
      AtomicInteger postResetCount = new AtomicInteger();
      serializer.submit(caseId, postResetCount::incrementAndGet, null);
      assertThat(postResetCount.get()).isEqualTo(1);
    }
  }

  @Test
  void reset_withNoInFlightWork_completesImmediately() {
    serializer.reset();

    AtomicInteger count = new AtomicInteger();
    serializer.submit(UUID.randomUUID(), count::incrementAndGet, null);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  void stressTest_concurrentSubmissionsWithInterleavedResets() throws Exception {
    int caseCount = 5;
    int writerCount = 10;
    int iterations = 100;
    UUID[] caseIds = new UUID[caseCount];
    for (int i = 0; i < caseCount; i++) {
      caseIds[i] = UUID.randomUUID();
    }

    AtomicInteger totalEvaluations = new AtomicInteger();
    AtomicInteger totalResets = new AtomicInteger();
    java.util.List<String> errors = new java.util.concurrent.CopyOnWriteArrayList<>();
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(writerCount + 1);

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int w = 0; w < writerCount; w++) {
        executor.submit(
            () -> {
              try {
                startLatch.await();
                for (int i = 0; i < iterations; i++) {
                  UUID caseId = caseIds[i % caseCount];
                  UUID signalId = UUID.randomUUID();
                  try {
                    Set<UUID> signals =
                        serializer.submit(caseId, totalEvaluations::incrementAndGet, signalId);
                    if (signals == null) {
                      errors.add("submit returned null");
                    }
                  } catch (Exception e) {
                    errors.add("Submit error: " + e.getMessage());
                  }
                }
              } catch (Exception e) {
                errors.add("Writer error: " + e.getMessage());
              } finally {
                doneLatch.countDown();
              }
            });
      }

      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int i = 0; i < 10; i++) {
                Thread.sleep(5);
                try {
                  serializer.reset();
                  totalResets.incrementAndGet();
                } catch (Exception e) {
                  errors.add("Reset error: " + e.getMessage());
                }
              }
            } catch (Exception e) {
              errors.add("Resetter error: " + e.getMessage());
            } finally {
              doneLatch.countDown();
            }
          });

      startLatch.countDown();
      assertThat(doneLatch.await(60, TimeUnit.SECONDS)).isTrue();
    }

    assertThat(errors).as("No errors during concurrent stress test").isEmpty();
    assertThat(totalEvaluations.get()).as("Some evaluations should have run").isGreaterThan(0);
    assertThat(totalResets.get()).as("Some resets should have run").isGreaterThan(0);
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
