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

import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImprovementOutcomeWorkerFunction {

  @SuppressWarnings("unchecked")
  public static WorkerFunction<Map, Map> create() {
    return new WorkerFunction.Sync<>(
        Map.class,
        Map.class,
        (input, scope) -> {
          var improvement = new LinkedHashMap<String, Object>();

          improvement.put("parentCaseId", stringOrNull(input, "parentCaseId"));
          improvement.put("category", stringOrNull(input, "category"));
          improvement.put("target", stringOrNull(input, "target"));

          String status = stringOrNull(input, "status");
          if (status == null) {
            status = stringOrNull(input, "reviewOutcome");
            if ("approved".equals(status)) {
              status = "MERGED";
            } else if ("rejected".equals(status)) {
              status = "REJECTED";
            } else {
              status = "MERGED";
            }
          }
          improvement.put("status", status);

          var prUrl = stringOrNull(input, "prUrl");
          if (prUrl != null) {
            improvement.put("prUrl", prUrl);
          }

          var targetPaths = input.get("targetPaths");
          if (targetPaths != null) {
            improvement.put("targetPaths", targetPaths);
          }

          var result = new LinkedHashMap<String, Object>();
          result.put("_improvement", improvement);
          return WorkerResult.of(result);
        });
  }

  private static String stringOrNull(Map<?, ?> map, String key) {
    var val = map.get(key);
    return val != null ? val.toString() : null;
  }
}
