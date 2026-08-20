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

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.FailureCategory;
import io.casehub.api.model.TaskStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MetaReasoningContextTest {

  private AdaptationContext minimalContext() {
    return new AdaptationContext(
        UUID.randomUUID(),
        "tenant-1",
        "comp-1",
        "goal-1",
        List.of(),
        List.of(),
        List.of(),
        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
        new CaseDefinition("ns", "name", "1.0"),
        TaskStatus.COMPLETED,
        "binding-1",
        0);
  }

  @Test
  void requires_adaptationContext() {
    assertThatThrownBy(() -> new MetaReasoningContext(null, 0, 3, 2, 5, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejects_negative_adaptationCount() {
    assertThatThrownBy(() -> new MetaReasoningContext(minimalContext(), -1, 3, 2, 5, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void remainingRatio_computed_correctly() {
    var ctx = new MetaReasoningContext(minimalContext(), 2, 3, 7, 10, null);
    assertThat(ctx.remainingRatio()).isEqualTo(0.7);
  }

  @Test
  void remainingRatio_zero_when_totalStepCount_zero() {
    var ctx = new MetaReasoningContext(minimalContext(), 0, 0, 0, 0, null);
    assertThat(ctx.remainingRatio()).isEqualTo(0.0);
  }

  @Test
  void latestFailureCategory_nullable() {
    var ctx = new MetaReasoningContext(minimalContext(), 0, 3, 2, 5, null);
    assertThat(ctx.latestFailureCategory()).isNull();
  }

  @Test
  void latestFailureCategory_carries_value() {
    var category = new FailureCategory.Knowledge("missing data", "entityId");
    var ctx = new MetaReasoningContext(minimalContext(), 1, 3, 2, 5, category);
    assertThat(ctx.latestFailureCategory()).isInstanceOf(FailureCategory.Knowledge.class);
  }
}
