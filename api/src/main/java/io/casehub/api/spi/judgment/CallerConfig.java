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

import io.casehub.api.spi.QuorumConfig;
import io.casehub.api.spi.routing.CandidateSetSpec;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Declares who can fulfill a judgment request. Used by {@link io.casehub.api.model.JudgmentTarget}
 * to specify the initial caller type and by {@link EscalationDecision.Escalate} to specify the
 * escalation target.
 *
 * <p>Refs engine#1012, engine#1009, engine#994.
 */
public sealed interface CallerConfig {

  static Human human() {
    return new Human(null, null, null, null, null, null, null, null, null, null, null, null);
  }

  record Human(
      @Nullable CandidateSetSpec candidateGroups,
      @Nullable CandidateSetSpec candidateUsers,
      @Nullable String title,
      @Nullable ExpressionEvaluator titleExpression,
      @Nullable Set<String> outcomes,
      @Nullable Integer claimDeadlineHours,
      @Nullable String scope,
      @Nullable ExpressionEvaluator scopeExpression,
      @Nullable String priority,
      @Nullable String templateRef,
      @Nullable Class<?> payloadType,
      @Nullable QuorumConfig quorum)
      implements CallerConfig {
    public Human {
      if (outcomes != null) outcomes = Set.copyOf(outcomes);
    }
  }

  record Llm(@Nullable String modelId, @Nullable String modelName, @Nullable String systemPrompt)
      implements CallerConfig {
    public Llm() {
      this(null, null, null);
    }
  }

  record A2A(String endpoint, @Nullable String skill, boolean streaming) implements CallerConfig {
    public A2A(String endpoint) {
      this(endpoint, null, false);
    }

    public A2A(String endpoint, @Nullable String skill) {
      this(endpoint, skill, false);
    }
  }

  record Any() implements CallerConfig {}
}
