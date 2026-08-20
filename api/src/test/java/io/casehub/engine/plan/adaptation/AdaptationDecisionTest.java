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
package io.casehub.engine.plan.adaptation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdaptationDecisionTest {

  @Test
  void persist_requires_reason() {
    assertThatThrownBy(() -> new AdaptationDecision.Persist(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void persist_carries_reason() {
    var decision = new AdaptationDecision.Persist("divergence below threshold");
    assertThat(decision.reason()).isEqualTo("divergence below threshold");
  }

  @Test
  void refine_requires_scope_and_reason() {
    assertThatThrownBy(() -> new AdaptationDecision.Refine(null, "reason"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new AdaptationDecision.Refine(RefineScope.LOCAL, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void refine_carries_scope_and_reason() {
    var decision = new AdaptationDecision.Refine(RefineScope.COMPOUND, "knowledge failure");
    assertThat(decision.scope()).isEqualTo(RefineScope.COMPOUND);
    assertThat(decision.reason()).isEqualTo("knowledge failure");
  }

  @Test
  void concede_requires_reason_and_compoundId() {
    assertThatThrownBy(() -> new AdaptationDecision.Concede(null, "comp"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new AdaptationDecision.Concede("reason", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void concede_carries_reason_and_compoundId() {
    var decision = new AdaptationDecision.Concede("ceiling reached", "investigation");
    assertThat(decision.reason()).isEqualTo("ceiling reached");
    assertThat(decision.compoundId()).isEqualTo("investigation");
  }

  @Test
  void pattern_matching_works() {
    AdaptationDecision decision = new AdaptationDecision.Refine(RefineScope.LOCAL, "test");
    String result =
        switch (decision) {
          case AdaptationDecision.Persist p -> "persist: " + p.reason();
          case AdaptationDecision.Refine r -> "refine: " + r.scope();
          case AdaptationDecision.Concede c -> "concede: " + c.compoundId();
        };
    assertThat(result).isEqualTo("refine: LOCAL");
  }
}
