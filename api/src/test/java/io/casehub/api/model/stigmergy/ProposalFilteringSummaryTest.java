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
package io.casehub.api.model.stigmergy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ProposalFilteringSummaryTest {

  @Test
  void constructsWithAllFields() {
    var summary =
        new ProposalFilteringSummary(
            Map.of("signal-consensus", 5, "trading-source", 3), 8, 7, 6, 5, 4, 3, 2);
    assertThat(summary.proposalsBySource()).containsEntry("signal-consensus", 5);
    assertThat(summary.proposalsBySource()).containsEntry("trading-source", 3);
    assertThat(summary.afterCategoryFilter()).isEqualTo(8);
    assertThat(summary.afterSuppressionFilter()).isEqualTo(7);
    assertThat(summary.afterAntiOscillationFilter()).isEqualTo(6);
    assertThat(summary.afterDenyFilter()).isEqualTo(5);
    assertThat(summary.afterBudgetFilter()).isEqualTo(4);
    assertThat(summary.afterConflictFilter()).isEqualTo(3);
    assertThat(summary.proposed()).isEqualTo(2);
  }

  @Test
  void emptySourceMapIsValid() {
    var summary = new ProposalFilteringSummary(Map.of(), 0, 0, 0, 0, 0, 0, 0);
    assertThat(summary.proposalsBySource()).isEmpty();
    assertThat(summary.proposed()).isZero();
  }
}
