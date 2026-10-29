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
package io.casehub.engine.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlowExecutionRegistryTest {

  private FlowExecutionRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new FlowExecutionRegistry();
  }

  @Test
  void register_then_get_returns_the_registered_execution() {
    UUID caseId = UUID.randomUUID();

    registry.register("id-1", caseId, "my-worker", "hash-abc");
    final FlowExecution execution = registry.get("id-1");

    assertThat(execution.caseId()).isEqualTo(caseId);
    assertThat(execution.workerName()).isEqualTo("my-worker");
    assertThat(execution.inputDataHash()).isEqualTo("hash-abc");
  }

  @Test
  void get_after_remove_throws_IllegalStateException() {
    UUID caseId = UUID.randomUUID();

    registry.register("id-2", caseId, "worker", "hash");
    registry.remove("id-2");

    assertThatThrownBy(() -> registry.get("id-2"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("id-2");
  }

  @Test
  void get_for_unknown_id_throws_IllegalStateException() {
    assertThatThrownBy(() -> registry.get("unknown"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unknown");
  }

  @Test
  void remove_is_idempotent() {
    UUID caseId = UUID.randomUUID();
    registry.register("id-3", caseId, "worker", "hash");

    registry.remove("id-3");
    registry.remove("id-3");
  }

  @Test
  void find_returns_empty_for_unregistered_instance() {
    assertThat(registry.find("nonexistent")).isEmpty();
  }

  @Test
  void find_returns_execution_for_registered_instance() {
    UUID caseId = UUID.randomUUID();
    registry.register("wf-1", caseId, "worker-1", "hash-1");

    var result = registry.find("wf-1");
    assertThat(result).isPresent();
    assertThat(result.get().caseId()).isEqualTo(caseId);
  }

  @Test
  void records_and_retrieves_step_results() {
    registry.register("wf-1", UUID.randomUUID(), "w", "h");
    var step =
        new io.casehub.api.model.StepResult(
            "s1",
            "fetch",
            io.casehub.api.model.StepStatus.SUCCEEDED,
            java.time.Duration.ofMillis(100),
            null);

    registry.recordStep("wf-1", step);

    List<io.casehub.api.model.StepResult> steps = registry.getSteps("wf-1");
    assertThat(steps).hasSize(1);
    assertThat(steps.getFirst().stepId()).isEqualTo("s1");
  }

  @Test
  void get_steps_returns_empty_when_no_steps_recorded() {
    assertThat(registry.getSteps("wf-unknown")).isEmpty();
  }

  @Test
  void remove_clears_step_data() {
    registry.register("wf-1", UUID.randomUUID(), "w", "h");
    registry.recordStep(
        "wf-1",
        new io.casehub.api.model.StepResult(
            "s1", "s", io.casehub.api.model.StepStatus.SUCCEEDED, java.time.Duration.ZERO, null));

    registry.remove("wf-1");

    assertThat(registry.find("wf-1")).isEmpty();
    assertThat(registry.getSteps("wf-1")).isEmpty();
  }

  @Test
  void concurrent_registrations_with_different_ids_do_not_cross_contaminate()
      throws InterruptedException {
    final int threads = 8;
    final CountDownLatch ready = new CountDownLatch(threads);
    final CountDownLatch go = new CountDownLatch(1);
    final List<Throwable> errors = new ArrayList<>();
    final ExecutorService pool = Executors.newFixedThreadPool(threads);

    for (int i = 0; i < threads; i++) {
      final String id = "concurrent-" + i;
      final String worker = "worker-" + i;
      final UUID caseId = UUID.randomUUID();
      pool.submit(
          () -> {
            ready.countDown();
            try {
              go.await();
              registry.register(id, caseId, worker, "hash-" + id);
              final FlowExecution exec = registry.get(id);
              if (!exec.workerName().equals(worker)) {
                errors.add(
                    new AssertionError(
                        "Cross-contamination: expected " + worker + " got " + exec.workerName()));
              }
              registry.remove(id);
            } catch (final Exception e) {
              errors.add(e);
            }
          });
    }

    ready.await();
    go.countDown();
    pool.shutdown();
    pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

    assertThat(errors).isEmpty();
  }
}
