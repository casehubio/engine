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
package io.casehub.engine.plan;

import java.util.List;
import java.util.Map;

public record PortfolioConfig(List<String> delegates, Map<String, Long> timeouts) {

  public static final List<String> DEFAULT_DELEGATES = List.of("goap", "llm");
  public static final long DEFAULT_TIMEOUT_MS = 30000L;
  public static final Map<String, Long> DEFAULT_TIMEOUTS = Map.of("goap", 1000L, "llm", 30000L);

  public PortfolioConfig {
    delegates =
        delegates == null || delegates.isEmpty() ? DEFAULT_DELEGATES : List.copyOf(delegates);
    timeouts = timeouts == null ? DEFAULT_TIMEOUTS : Map.copyOf(timeouts);
    for (var entry : timeouts.entrySet()) {
      if (entry.getValue() <= 0) {
        throw new IllegalArgumentException("timeout for '" + entry.getKey() + "' must be positive");
      }
    }
  }

  public static PortfolioConfig defaults() {
    return new PortfolioConfig(DEFAULT_DELEGATES, DEFAULT_TIMEOUTS);
  }

  public long timeoutFor(String strategyId) {
    return timeouts.getOrDefault(strategyId, DEFAULT_TIMEOUT_MS);
  }
}
