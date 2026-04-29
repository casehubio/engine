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

/**
 * Execution policy for worker tasks.
 *
 * <p>Defines timeout and retry behavior for worker execution.
 *
 * <p><b>Timeout:</b> If {@code timeoutMs} is {@code null}, the engine uses the default configured
 * via {@code casehub.engine.worker.default-timeout-ms} (default: 60000ms). Individual workers can
 * override this by specifying a non-null {@code timeoutMs} in their ExecutionPolicy.
 *
 * <p><b>Retries:</b> Retry policy controls automatic retry behavior on failure. See {@link
 * RetryPolicy} for details.
 *
 * @param timeoutMs worker execution timeout in milliseconds, or {@code null} to use configured
 *     default
 * @param retries retry policy for failed executions
 */
public record ExecutionPolicy(Integer timeoutMs, RetryPolicy retries) {

  /**
   * Default execution policy with system default timeout (configured via {@code
   * casehub.engine.worker.default-timeout-ms}) and default retry policy.
   */
  public ExecutionPolicy() {
    this(null, new RetryPolicy());
  }
}
