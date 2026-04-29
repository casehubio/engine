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
package io.casehub.model;

/**
 * Execution policy for worker tasks.
 *
 * <p>Defines timeout and retry behavior for worker execution.
 *
 * <p><b>Timeout:</b> If {@code timeoutMs} is {@code null} or not specified in YAML/JSON, the engine
 * uses the default configured via {@code casehub.engine.worker.default-timeout-ms} (default:
 * 60000ms). Individual workers can override this by specifying a {@code timeoutMs} value.
 *
 * <p><b>Retries:</b> Retry policy controls automatic retry behavior on failure. See {@link
 * RetryPolicy} for details.
 */
public class ExecutionPolicy {

  /**
   * Worker execution timeout in milliseconds, or {@code null} to use configured default.
   *
   * <p>When not specified in YAML/JSON definition, defaults to {@code null}, which means the engine
   * will use the system-wide default configured via {@code
   * casehub.engine.worker.default-timeout-ms}.
   */
  private Integer timeoutMs = null;

  /** Retry policy for failed executions. */
  private RetryPolicy retries = new RetryPolicy();

  public Integer getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(Integer timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public RetryPolicy getRetries() {
    return retries;
  }

  public void setRetries(RetryPolicy retries) {
    this.retries = retries;
  }
}
