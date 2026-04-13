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
package io.casehub.api.engine;

import io.casehub.api.model.CaseHubDefinition;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public abstract class CaseHub {

  @Inject CaseHubRuntime runtime;

  public abstract CaseHubDefinition getDefinition();

  public CompletionStage<UUID> startCase() {
    return runtime.startCase(getDefinition());
  }

  public CompletionStage<UUID> startCase(Map<String, Object> inputData) {
    return runtime.startCase(getDefinition(), inputData);
  }

  public void signal(UUID caseId, String path, Object value) {
    runtime.signal(caseId, path, value);
  }

  public CompletionStage<Object> query(UUID caseId, String path) {
    return runtime.query(caseId, path);
  }

  public <T> CompletionStage<T> query(UUID caseId, String path, Class<T> clazz) {
    return runtime.query(caseId, path, clazz);
  }
}
