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
package io.casehub.engine.yamlcbr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.PlaybookExecutionResult;
import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaybookOutcomeRecorderTest {

  private StepExecutionCbrBridgeTest.RecordingCbrStore cbrStore;
  private PlaybookOutcomeRecorder recorder;

  @BeforeEach
  void setUp() {
    cbrStore = new StepExecutionCbrBridgeTest.RecordingCbrStore();
    recorder = new PlaybookOutcomeRecorder(cbrStore);
  }

  @Test
  void recordsSuccessfulExecution() {
    var step =
        new StepResult("s1", "fetch-data", StepStatus.SUCCEEDED, Duration.ofMillis(150), null);
    var result = new PlaybookExecutionResult.Success(Map.of("key", "value"), List.of(step));

    recorder.record(UUID.randomUUID(), "tenant-1", "test-case", "binding-1", result, 0, List.of());

    assertEquals(1, cbrStore.stored.size());
    var stored = cbrStore.stored.getFirst().record();
    assertInstanceOf(CbrPlanRecord.class, stored);
    assertEquals(FeatureValue.of("SUCCESS"), stored.features().get("outcome"));
    assertEquals(FeatureValue.of(1), stored.features().get("stepCount"));
    assertEquals(FeatureValue.of(0), stored.features().get("triageRound"));
  }

  @Test
  void recordsFailedExecution() {
    var step1 = new StepResult("s1", "fetch", StepStatus.SUCCEEDED, Duration.ofMillis(100), null);
    var step2 = new StepResult("s2", "transform", StepStatus.FAILED, Duration.ofMillis(10), "NPE");
    var result = new PlaybookExecutionResult.Failure("NPE in transform", List.of(step1, step2));

    recorder.record(UUID.randomUUID(), "tenant-1", "test-case", "binding-1", result, 0, List.of());

    assertEquals(1, cbrStore.stored.size());
    var stored = cbrStore.stored.getFirst().record();
    assertEquals(FeatureValue.of("FAILURE"), stored.features().get("outcome"));
    assertEquals(FeatureValue.of("transform"), stored.features().get("failedStepName"));
    assertEquals(FeatureValue.of("NPE in transform"), stored.features().get("rootCause"));
  }

  @Test
  void recordsTriageChainContext() {
    var step = new StepResult("s1", "fetch", StepStatus.SUCCEEDED, Duration.ofMillis(100), null);
    var result = new PlaybookExecutionResult.Success(Map.of(), List.of(step));
    var previousMods = List.of(Map.<String, Object>of("playbook.skipSteps", List.of("s2")));

    recorder.record(
        UUID.randomUUID(), "tenant-1", "test-case", "binding-1", result, 1, previousMods);

    var stored = cbrStore.stored.getFirst().record();
    assertEquals(FeatureValue.of(1), stored.features().get("triageRound"));
    assertTrue(stored.features().containsKey("triageModificationCount"));
  }

  @Test
  void recordsPartialSuccess() {
    var step1 = new StepResult("s1", "fetch", StepStatus.SUCCEEDED, Duration.ofMillis(200), null);
    var step2 =
        new StepResult("s2", "process", StepStatus.FAILED, Duration.ofMillis(10), "timeout");
    var result =
        new PlaybookExecutionResult.PartialSuccess(Map.of("partial", true), List.of(step1, step2));

    recorder.record(UUID.randomUUID(), "tenant-1", "test-case", "binding-1", result, 0, List.of());

    var stored = cbrStore.stored.getFirst().record();
    assertEquals(FeatureValue.of("PARTIAL_SUCCESS"), stored.features().get("outcome"));
    assertEquals(FeatureValue.of(2), stored.features().get("stepCount"));
  }

  @Test
  void stepsPopulatedAsPlanSteps() {
    var step1 = new StepResult("s1", "fetch", StepStatus.SUCCEEDED, Duration.ofMillis(100), null);
    var step2 = new StepResult("s2", "transform", StepStatus.FAILED, Duration.ofMillis(10), "NPE");
    var result = new PlaybookExecutionResult.Failure("NPE", List.of(step1, step2));

    recorder.record(UUID.randomUUID(), "tenant-1", "test-case", "binding-1", result, 0, List.of());

    var stored = (CbrPlanRecord) cbrStore.stored.getFirst().record();
    assertEquals(2, stored.cbrPlanStep().size());
    assertEquals("fetch", stored.cbrPlanStep().getFirst().capabilityName());
    assertEquals("SUCCEEDED", stored.cbrPlanStep().getFirst().stepOutcome());
    assertEquals("FAILED", stored.cbrPlanStep().get(1).stepOutcome());
  }
}
