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
package io.casehub.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaybookExecutionResultTest {

  @Test
  void success_contains_output_and_steps() {
    var step =
        new StepResult("s1", "fetch-data", StepStatus.SUCCEEDED, Duration.ofMillis(150), null);
    var result = new PlaybookExecutionResult.Success(Map.of("key", "value"), List.of(step));

    assertThat(result.steps()).hasSize(1);
    assertThat(result.steps().getFirst().stepName()).isEqualTo("fetch-data");
    assertThat(result.output()).isNotNull();
    assertThat(result).isInstanceOf(PlaybookExecutionResult.class);
  }

  @Test
  void failure_contains_root_cause_and_failed_step() {
    var step1 =
        new StepResult("s1", "fetch-data", StepStatus.SUCCEEDED, Duration.ofMillis(100), null);
    var step2 =
        new StepResult(
            "s2", "transform", StepStatus.FAILED, Duration.ofMillis(50), "NullPointerException");
    var result = new PlaybookExecutionResult.Failure("transform step NPE", List.of(step1, step2));

    assertThat(result.rootCause()).isEqualTo("transform step NPE");
    assertThat(result.steps()).hasSize(2);
    assertThat(result.steps().get(1).status()).isEqualTo(StepStatus.FAILED);
    assertThat(result.steps().get(1).error()).isEqualTo("NullPointerException");
  }

  @Test
  void partial_success_contains_partial_output_and_mixed_steps() {
    var step1 = new StepResult("s1", "fetch", StepStatus.SUCCEEDED, Duration.ofMillis(200), null);
    var step2 =
        new StepResult("s2", "process", StepStatus.FAILED, Duration.ofMillis(10), "timeout");
    var result =
        new PlaybookExecutionResult.PartialSuccess(Map.of("partial", true), List.of(step1, step2));

    assertThat(result.partialOutput()).isNotNull();
    assertThat(result.steps()).hasSize(2);
  }

  @Test
  void step_result_skipped_status() {
    var step = new StepResult("s3", "optional-step", StepStatus.SKIPPED, Duration.ZERO, null);
    assertThat(step.status()).isEqualTo(StepStatus.SKIPPED);
    assertThat(step.error()).isNull();
  }
}
