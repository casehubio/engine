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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.CaseHub;
import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.engine.SettlementTimeoutException;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link CaseHubRuntime#awaitQuiescence(UUID, Duration)} tracks all cascading waves of
 * worker execution — not just the first wave like {@code signalAndAwait}.
 *
 * <p>Refs casehubio/engine#610.
 */
@QuarkusTest
class QuiescenceIntegrationTest {

  @Inject CaseHubRuntime runtime;
  @Inject TwoStepQuiescenceBean twoStepBean;
  @Inject ThreeStepQuiescenceBean threeStepBean;
  @Inject SlowWorkerBean slowWorkerBean;
  @Inject SingleWorkerBean singleWorkerBean;

  @Test
  void awaitQuiescence_waitsForCascadingWorkers() {
    UUID caseId = twoStepBean.startCase(Map.of());

    runtime.signal(caseId, Map.of("trigger", "go"));
    CaseContext ctx = runtime.awaitQuiescence(caseId, Duration.ofSeconds(15));

    assertThat(ctx.get("step1")).isEqualTo("done");
    assertThat(ctx.get("step2")).isEqualTo("done");
  }

  @Test
  void awaitQuiescence_singleWorker() {
    UUID caseId = singleWorkerBean.startCase(Map.of());

    runtime.signal(caseId, Map.of("trigger", "go"));
    CaseContext ctx = runtime.awaitQuiescence(caseId, Duration.ofSeconds(15));

    assertThat(ctx.get("result")).isEqualTo("done");
  }

  @Test
  void awaitQuiescence_threeStepCascade() {
    UUID caseId = threeStepBean.startCase(Map.of());

    runtime.signal(caseId, Map.of("trigger", "go"));
    CaseContext ctx = runtime.awaitQuiescence(caseId, Duration.ofSeconds(15));

    assertThat(ctx.get("step1")).isEqualTo("done");
    assertThat(ctx.get("step2")).isEqualTo("done");
    assertThat(ctx.get("step3")).isEqualTo("done");
  }

  @Test
  void awaitQuiescence_throwsOnTimeout() {
    UUID caseId = slowWorkerBean.startCase(Map.of());

    runtime.signal(caseId, Map.of("trigger", "go"));

    assertThatThrownBy(() -> runtime.awaitQuiescence(caseId, Duration.ofMillis(200)))
        .isInstanceOf(SettlementTimeoutException.class);
  }

  @Test
  void awaitQuiescence_calledTwiceAfterSeparateSignals() {
    UUID caseId = singleWorkerBean.startCase(Map.of());

    runtime.signal(caseId, Map.of("trigger", "go"));
    CaseContext ctx1 = runtime.awaitQuiescence(caseId, Duration.ofSeconds(15));
    assertThat(ctx1.get("result")).isEqualTo("done");

    runtime.signal(caseId, Map.of("trigger2", "go"));
    CaseContext ctx2 = runtime.awaitQuiescence(caseId, Duration.ofSeconds(15));
    assertThat(ctx2.get("result")).isEqualTo("done");
  }

  @ApplicationScoped
  public static class SingleWorkerBean extends CaseHub {
    @Override
    public CaseDefinition getDefinition() {
      Capability cap =
          Capability.builder()
              .name("work")
              .inputSchema(".")
              .outputSchema("{ result: \"done\" }")
              .build();

      Worker worker =
          Worker.builder()
              .name("singleWorker")
              .capabilityName("work")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> WorkerResult.of(Map.of("result", "done"))))
              .build();

      return CaseDefinition.builder()
          .namespace("test")
          .name("SingleWorkerQuiescence")
          .version("1.0.0")
          .capabilities(cap)
          .workers(worker)
          .bindings(
              Binding.builder()
                  .name("doWork")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".trigger != null"))
                  .build())
          .build();
    }
  }

  @ApplicationScoped
  public static class SlowWorkerBean extends CaseHub {
    static final CountDownLatch LATCH = new CountDownLatch(1);

    @Override
    public CaseDefinition getDefinition() {
      Capability cap =
          Capability.builder()
              .name("slowWork")
              .inputSchema(".")
              .outputSchema("{ slow: \"done\" }")
              .build();

      Worker worker =
          Worker.builder()
              .name("slowWorker")
              .capabilityName("slowWork")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> {
                        try {
                          LATCH.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                        }
                        return WorkerResult.of(Map.of("slow", "done"));
                      }))
              .build();

      return CaseDefinition.builder()
          .namespace("test")
          .name("SlowWorkerQuiescence")
          .version("1.0.0")
          .capabilities(cap)
          .workers(worker)
          .bindings(
              Binding.builder()
                  .name("doSlowWork")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".trigger != null"))
                  .build())
          .build();
    }
  }

  @ApplicationScoped
  public static class ThreeStepQuiescenceBean extends CaseHub {
    @Override
    public CaseDefinition getDefinition() {
      Capability cap1 =
          Capability.builder()
              .name("threeStep1Cap")
              .inputSchema(".")
              .outputSchema("{ step1: \"done\" }")
              .build();
      Capability cap2 =
          Capability.builder()
              .name("threeStep2Cap")
              .inputSchema(".")
              .outputSchema("{ step2: \"done\" }")
              .build();
      Capability cap3 =
          Capability.builder()
              .name("threeStep3Cap")
              .inputSchema(".")
              .outputSchema("{ step3: \"done\" }")
              .build();

      Worker w1 =
          Worker.builder()
              .name("threeStep1Worker")
              .capabilityName("threeStep1Cap")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> WorkerResult.of(Map.of("step1", "done"))))
              .build();
      Worker w2 =
          Worker.builder()
              .name("threeStep2Worker")
              .capabilityName("threeStep2Cap")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> WorkerResult.of(Map.of("step2", "done"))))
              .build();
      Worker w3 =
          Worker.builder()
              .name("threeStep3Worker")
              .capabilityName("threeStep3Cap")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> WorkerResult.of(Map.of("step3", "done"))))
              .build();

      return CaseDefinition.builder()
          .namespace("test")
          .name("ThreeStepQuiescence")
          .version("1.0.0")
          .capabilities(cap1, cap2, cap3)
          .workers(w1, w2, w3)
          .bindings(
              Binding.builder()
                  .name("trigStep1")
                  .capability(cap1)
                  .on(new ContextChangeTrigger(".trigger != null"))
                  .build(),
              Binding.builder()
                  .name("trigStep2")
                  .capability(cap2)
                  .on(new ContextChangeTrigger(".step1 == \"done\""))
                  .build(),
              Binding.builder()
                  .name("trigStep3")
                  .capability(cap3)
                  .on(new ContextChangeTrigger(".step2 == \"done\""))
                  .build())
          .build();
    }
  }

  /**
   * Two-step cascading case: step1 fires on {@code .trigger}, producing {@code step1="done"}. step2
   * fires on {@code .step1 == "done"}, producing {@code step2="done"}. After signalAndAwait, only
   * step1 is guaranteed; awaitQuiescence guarantees both.
   */
  @ApplicationScoped
  public static class TwoStepQuiescenceBean extends CaseHub {
    @Override
    public CaseDefinition getDefinition() {
      Capability cap1 =
          Capability.builder()
              .name("step1Cap")
              .inputSchema(".")
              .outputSchema("{ step1: \"done\" }")
              .build();

      Capability cap2 =
          Capability.builder()
              .name("step2Cap")
              .inputSchema(".")
              .outputSchema("{ step2: \"done\" }")
              .build();

      Worker worker1 =
          Worker.builder()
              .name("step1Worker")
              .capabilityName("step1Cap")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> WorkerResult.of(Map.of("step1", "done"))))
              .build();

      Worker worker2 =
          Worker.builder()
              .name("step2Worker")
              .capabilityName("step2Cap")
              .function(
                  new WorkerFunction.Sync<>(
                      Map.class,
                      Map.class,
                      (input, scope) -> WorkerResult.of(Map.of("step2", "done"))))
              .build();

      return CaseDefinition.builder()
          .namespace("test")
          .name("TwoStepQuiescence")
          .version("1.0.0")
          .capabilities(cap1, cap2)
          .workers(worker1, worker2)
          .bindings(
              Binding.builder()
                  .name("triggerStep1")
                  .capability(cap1)
                  .on(new ContextChangeTrigger(".trigger != null"))
                  .build(),
              Binding.builder()
                  .name("triggerStep2")
                  .capability(cap2)
                  .on(new ContextChangeTrigger(".step1 == \"done\""))
                  .build())
          .build();
    }
  }
}
