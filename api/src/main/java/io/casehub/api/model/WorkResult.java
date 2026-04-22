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

import java.util.Map;

/**
 * Result of orchestrated work returned by {@code WorkOrchestrator.submit()}. The {@code
 * correlationKey} is the idempotency hash used to match this result to its submission.
 */
public record WorkResult(
    String correlationKey, WorkStatus status, Map<String, Object> output, String workerId) {

  public static WorkResult completed(
      String correlationKey, Map<String, Object> output, String workerId) {
    return new WorkResult(correlationKey, WorkStatus.COMPLETED, output, workerId);
  }

  public static WorkResult faulted(String correlationKey, String workerId) {
    return new WorkResult(correlationKey, WorkStatus.FAULTED, Map.of(), workerId);
  }
}
