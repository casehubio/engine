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
package io.casehub.engine.plan.monitoring;

public record MonitoringConfig(boolean enabled, double perCompletionThreshold, int windowSize) {

  public static final double DEFAULT_THRESHOLD = 0.5;
  public static final int DEFAULT_WINDOW_SIZE = 5;

  public MonitoringConfig {
    if (perCompletionThreshold < 0.0 || perCompletionThreshold > 1.0) {
      throw new IllegalArgumentException("perCompletionThreshold must be in [0.0, 1.0]");
    }
    if (windowSize < 1) {
      throw new IllegalArgumentException("windowSize must be >= 1");
    }
  }

  public static MonitoringConfig defaults() {
    return new MonitoringConfig(true, DEFAULT_THRESHOLD, DEFAULT_WINDOW_SIZE);
  }

  public static MonitoringConfig disabled() {
    return new MonitoringConfig(false, DEFAULT_THRESHOLD, DEFAULT_WINDOW_SIZE);
  }
}
