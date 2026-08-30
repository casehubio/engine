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
package io.casehub.api.spi.judgment;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Declares who can fulfill a judgment request. Used by {@link io.casehub.api.model.JudgmentTarget}
 * to specify the initial caller type and by {@link EscalationDecision.Escalate} to specify the
 * escalation target.
 *
 * <p>Refs engine#1009, engine#994.
 */
public sealed interface CallerConfig {

  record Human(List<String> candidateGroups, @Nullable String minimumTrustLevel)
      implements CallerConfig {
    public Human {
      candidateGroups = List.copyOf(candidateGroups);
    }

    public Human(List<String> candidateGroups) {
      this(candidateGroups, null);
    }
  }

  record Llm(@Nullable String modelId) implements CallerConfig {
    public Llm() {
      this(null);
    }
  }

  record A2A(String endpoint, @Nullable String skill) implements CallerConfig {
    public A2A(String endpoint) {
      this(endpoint, null);
    }
  }

  record Any() implements CallerConfig {}
}
