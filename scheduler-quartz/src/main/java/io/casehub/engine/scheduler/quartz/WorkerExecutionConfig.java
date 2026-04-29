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
package io.casehub.engine.scheduler.quartz;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuration for worker execution behavior.
 *
 * <p>Provides default timeout and other execution-related settings that can be overridden per
 * worker via {@link io.casehub.api.model.ExecutionPolicy}.
 */
@ApplicationScoped
class WorkerExecutionConfig {

  @ConfigProperty(name = "casehub.engine.worker.default-timeout-ms", defaultValue = "60000")
  int defaultTimeoutMs;

  /**
   * Returns the effective timeout for a worker execution.
   *
   * <p>If the worker's ExecutionPolicy specifies a timeout, uses that. Otherwise uses the
   * configured default.
   *
   * @param workerTimeoutMs worker-specific timeout from ExecutionPolicy, or null
   * @return effective timeout in milliseconds
   */
  int getEffectiveTimeout(Integer workerTimeoutMs) {
    return workerTimeoutMs != null ? workerTimeoutMs : defaultTimeoutMs;
  }
}
