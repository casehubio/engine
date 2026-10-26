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
package io.casehub.engine.runtime.improvement.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.casehub.api.engine.WorkerRuntime;
import io.casehub.api.model.WorkerContext;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerOutcome;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImprovementOutcomeWorkerFunctionTest {

  @SuppressWarnings("unchecked")
  @Test
  void producesImprovementContextKey() {
    var fn = (WorkerFunction.Sync<Map, Map>) ImprovementOutcomeWorkerFunction.create();
    var runtime = mock(WorkerRuntime.class);
    var workerContext =
        new WorkerContext("test", UUID.randomUUID(), null, null, null, Map.of(), null, null, null);
    when(runtime.context()).thenReturn(workerContext);

    var input =
        Map.of(
            "parentCaseId", UUID.randomUUID().toString(),
            "category", "dependency-update",
            "target", "hibernate",
            "status", "MERGED",
            "prUrl", "https://github.com/org/repo/pull/42");

    var result = fn.fn().apply(input, runtime);

    assertThat(result.outcome()).isInstanceOf(WorkerOutcome.Success.class);
    var output = result.output();
    assertThat(output).containsKey("_improvement");

    @SuppressWarnings("unchecked")
    var improvement = (Map<String, Object>) output.get("_improvement");
    assertThat(improvement.get("category")).isEqualTo("dependency-update");
    assertThat(improvement.get("target")).isEqualTo("hibernate");
    assertThat(improvement.get("status")).isEqualTo("MERGED");
    assertThat(improvement.get("prUrl")).isEqualTo("https://github.com/org/repo/pull/42");
  }

  @SuppressWarnings("unchecked")
  @Test
  void infersStatusFromReviewOutcome() {
    var fn = (WorkerFunction.Sync<Map, Map>) ImprovementOutcomeWorkerFunction.create();
    var runtime = mock(WorkerRuntime.class);
    var workerContext =
        new WorkerContext("test", UUID.randomUUID(), null, null, null, Map.of(), null, null, null);
    when(runtime.context()).thenReturn(workerContext);

    var input =
        Map.of(
            "parentCaseId", UUID.randomUUID().toString(),
            "category", "lint-fix",
            "target", "checkstyle",
            "reviewOutcome", "approved");

    var result = fn.fn().apply(input, runtime);

    @SuppressWarnings("unchecked")
    var improvement = (Map<String, Object>) result.output().get("_improvement");
    assertThat(improvement.get("status")).isEqualTo("MERGED");
  }

  @SuppressWarnings("unchecked")
  @Test
  void rejectedReviewMapsToRejected() {
    var fn = (WorkerFunction.Sync<Map, Map>) ImprovementOutcomeWorkerFunction.create();
    var runtime = mock(WorkerRuntime.class);
    var workerContext =
        new WorkerContext("test", UUID.randomUUID(), null, null, null, Map.of(), null, null, null);
    when(runtime.context()).thenReturn(workerContext);

    var input =
        Map.of(
            "parentCaseId", UUID.randomUUID().toString(),
            "category", "lint-fix",
            "target", "checkstyle",
            "reviewOutcome", "rejected");

    var result = fn.fn().apply(input, runtime);

    @SuppressWarnings("unchecked")
    var improvement = (Map<String, Object>) result.output().get("_improvement");
    assertThat(improvement.get("status")).isEqualTo("REJECTED");
  }
}
