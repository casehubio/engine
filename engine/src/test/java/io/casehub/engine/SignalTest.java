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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.model.CaseState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SignalTest {

  @Inject SignalCaseHubBean bean;

  @Inject TwoSignalCaseHubBean twoSignalBean;

  @Inject CaseInstanceCache caseInstanceCache;

  @BeforeEach
  void reset() {
    SignalCaseHubBean.runCount.set(0);
    TwoSignalCaseHubBean.paymentRunCount.set(0);
    TwoSignalCaseHubBean.documentRunCount.set(0);
  }

  // ------------------------------------------------------------------ //

  /**
   * Case starts without the trigger field — worker must NOT run. After signal writes the field, the
   * rule fires and worker executes.
   */
  @Test
  void workerShouldNotRunBeforeSignalAndRunAfter() {
    UUID caseId = bean.startCase(Map.of("orderId", "order-1")).toCompletableFuture().join();

    assertNotNull(caseId);

    // worker must NOT run before signal
    await()
        .during(2, TimeUnit.SECONDS)
        .atMost(3, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertEquals(
                    0, SignalCaseHubBean.runCount.get(), "Worker must not run before signal"));

    // send signal — writes the triggering field
    bean.signal(caseId, "payment", Map.of("amount", 500, "currency", "USD"));

    // worker must run and case must complete
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertEquals(
                  1, SignalCaseHubBean.runCount.get(), "Worker must run exactly once after signal");
              assertEquals(CaseState.COMPLETED, caseInstanceCache.get(caseId).getState());
            });
  }

  /**
   * Worker must run exactly once even if the same signal is sent twice. The dedup mechanism should
   * prevent double execution.
   */
  @Test
  void workerRunsOnceOnDuplicateSignal() {
    UUID caseId = bean.startCase(Map.of("orderId", "order-dedup")).toCompletableFuture().join();

    bean.signal(caseId, "payment", Map.of("amount", 100, "currency", "EUR"));
    bean.signal(caseId, "payment", Map.of("amount", 100, "currency", "EUR"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertEquals(
                    1,
                    SignalCaseHubBean.runCount.get(),
                    "Worker must run exactly once despite duplicate signal"));
  }

  /** Signal with a primitive value (not a Map) must also trigger the rule. */
  @Test
  void signalWithPrimitiveValueTriggersWorker() {
    UUID caseId = bean.startCase(Map.of("orderId", "order-primitive")).toCompletableFuture().join();

    // signal writes a non-null value — rule checks `.payment != null`
    bean.signal(caseId, "payment", 999);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(1, SignalCaseHubBean.runCount.get()));
  }

  /**
   * Two independent signals trigger two different workers. Case completes only after both workers
   * finish.
   */
  @Test
  void twoIndependentSignalsTriggerTwoWorkers() {
    UUID caseId =
        twoSignalBean
            .startCase(Map.of("orderId", "order-two-signals"))
            .toCompletableFuture()
            .join();

    // neither worker should run yet
    await()
        .during(2, TimeUnit.SECONDS)
        .atMost(3, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertEquals(0, TwoSignalCaseHubBean.paymentRunCount.get());
              assertEquals(0, TwoSignalCaseHubBean.documentRunCount.get());
            });

    twoSignalBean.signal(caseId, "paymentApproved", Map.of("amount", 200));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(1, TwoSignalCaseHubBean.paymentRunCount.get()));
    assertEquals(
        0, TwoSignalCaseHubBean.documentRunCount.get(), "Document worker must not run yet");

    twoSignalBean.signal(caseId, "documentUploaded", Map.of("filename", "contract.pdf"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              assertEquals(1, TwoSignalCaseHubBean.paymentRunCount.get());
              assertEquals(1, TwoSignalCaseHubBean.documentRunCount.get());
              assertEquals(CaseState.COMPLETED, caseInstanceCache.get(caseId).getState());
            });
  }

  /** Signal written value must be readable through query after the signal is processed. */
  @Test
  void signalValueIsAvailableViaQuery() {
    UUID caseId = bean.startCase(Map.of("orderId", "order-query")).toCompletableFuture().join();

    bean.signal(caseId, "payment", Map.of("amount", 750, "currency", "GBP"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(1, SignalCaseHubBean.runCount.get()));

    // the worker output wrote "status" = "paid" into the context
    String status = bean.query(caseId, "status", String.class).toCompletableFuture().join();
    assertEquals("paid", status);
  }

  // ================================================================== //
  //  Beans                                                              //
  // ================================================================== //

  /**
   * Case with a single rule triggered by {@code .payment != null}. Starts without {@code payment}
   * in context so no worker fires immediately.
   */
  @ApplicationScoped
  public static class SignalCaseHubBean extends CaseHub {

    static final AtomicInteger runCount = new AtomicInteger();

    private final Capability paymentCapability =
        Capability.builder()
            .name("processPayment")
            .inputSchema("{ payment: .payment }")
            .outputSchema("{ status: .status }")
            .build();

    private final Goal paidGoal =
        Goal.builder()
            .name("paymentProcessed")
            .condition(".status == \"paid\"")
            .kind(GoalKind.SUCCESS)
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-signal")
          .name("Signal Test")
          .version("1.0.0")
          .capabilities(paymentCapability)
          .workers(
              Worker.builder()
                  .name("payment-worker")
                  .capabilities(paymentCapability)
                  .function(
                      input -> {
                        runCount.incrementAndGet();
                        return Map.of("status", "paid");
                      })
                  .build())
          .bindings(
              Binding.builder()
                  .name("on-payment-received")
                  .capability(paymentCapability)
                  .on(new ContextChangeTrigger(".payment != null"))
                  .build())
          .goals(paidGoal)
          .completion(GoalExpression.allOf(paidGoal))
          .build();
    }
  }

  /**
   * Case with two independent rules, each triggered by a different signal. Completes only after
   * both workers finish.
   */
  @ApplicationScoped
  public static class TwoSignalCaseHubBean extends CaseHub {

    static final AtomicInteger paymentRunCount = new AtomicInteger();
    static final AtomicInteger documentRunCount = new AtomicInteger();

    private final Capability paymentCapability =
        Capability.builder()
            .name("processPayment2")
            .inputSchema("{ paymentApproved: .paymentApproved }")
            .outputSchema("{ paymentProcessed: .paymentProcessed }")
            .build();

    private final Capability documentCapability =
        Capability.builder()
            .name("processDocument2")
            .inputSchema("{ documentUploaded: .documentUploaded }")
            .outputSchema("{ documentProcessed: .documentProcessed }")
            .build();

    private final Goal allDoneGoal =
        Goal.builder()
            .name("allDone")
            .condition(".paymentProcessed == true and .documentProcessed == true")
            .kind(GoalKind.SUCCESS)
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-signal-two")
          .name("Two Signal Test")
          .version("1.0.0")
          .capabilities(paymentCapability, documentCapability)
          .workers(
              Worker.builder()
                  .name("payment-worker-2")
                  .capabilities(paymentCapability)
                  .function(
                      input -> {
                        paymentRunCount.incrementAndGet();
                        return Map.of("paymentProcessed", true);
                      })
                  .build(),
              Worker.builder()
                  .name("document-worker-2")
                  .capabilities(documentCapability)
                  .function(
                      input -> {
                        documentRunCount.incrementAndGet();
                        return Map.of("documentProcessed", true);
                      })
                  .build())
          .bindings(
              Binding.builder()
                  .name("on-payment-approved")
                  .capability(paymentCapability)
                  .on(new ContextChangeTrigger(".paymentApproved != null"))
                  .build(),
              Binding.builder()
                  .name("on-document-uploaded")
                  .capability(documentCapability)
                  .on(new ContextChangeTrigger(".documentUploaded != null"))
                  .build())
          .goals(allDoneGoal)
          .completion(GoalExpression.allOf(allDoneGoal))
          .build();
    }
  }
}
