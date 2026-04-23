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
package io.casehub.engine.internal.worker;

import io.casehub.api.context.PropagationContext;
import io.casehub.api.model.WorkRequest;
import io.casehub.api.model.WorkerContext;
import io.casehub.api.spi.WorkerContextProvider;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

/**
 * Default WorkerContextProvider. Returns a minimal context with the task capability as the
 * description and an empty prior-workers list.
 */
@ApplicationScoped
public class EmptyWorkerContextProvider implements WorkerContextProvider {

  @Override
  public WorkerContext buildContext(String workerId, WorkRequest task) {
    return new WorkerContext(
        task.capability(), null, null, List.of(), PropagationContext.createRoot(), Map.of());
  }
}
