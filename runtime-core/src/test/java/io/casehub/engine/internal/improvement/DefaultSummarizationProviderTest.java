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
package io.casehub.engine.internal.improvement;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.stigmergy.SummaryScope;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSummarizationProviderTest {

  private DefaultSummarizationProvider provider;
  private UUID caseId;

  @BeforeEach
  void setUp() {
    provider = new DefaultSummarizationProvider();
    caseId = UUID.randomUUID();
  }

  @Test
  void projectWideSummaryWithNoEvents() {
    var scope = new SummaryScope(null, null, null, null);

    var summary = provider.summarize(caseId, "t1", scope);

    assertThat(summary).isNotNull();
    assertThat(summary.scope()).isEqualTo(scope);
    assertThat(summary.computedAt()).isNotNull();
    assertThat(summary.totalImprovements()).isZero();
    assertThat(summary.successCount()).isZero();
    assertThat(summary.failureCount()).isZero();
    assertThat(summary.categories()).isEmpty();
    assertThat(summary.researchDirections()).isEmpty();
    assertThat(summary.notableEvents()).isEmpty();
  }

  @Test
  void areaScopedSummary() {
    var scope = new SummaryScope("stability", null, null, null);

    var summary = provider.summarize(caseId, "t1", scope);

    assertThat(summary).isNotNull();
    assertThat(summary.scope().areaId()).isEqualTo("stability");
  }

  @Test
  void categoryScopedSummary() {
    var scope = new SummaryScope(null, "dependency-update", null, null);

    var summary = provider.summarize(caseId, "t1", scope);

    assertThat(summary).isNotNull();
    assertThat(summary.scope().category()).isEqualTo("dependency-update");
  }

  @Test
  void timeWindowScopedSummary() {
    var scope = new SummaryScope(null, null, 60, null);

    var summary = provider.summarize(caseId, "t1", scope);

    assertThat(summary).isNotNull();
    assertThat(summary.scope().timeWindowMinutes()).isEqualTo(60);
  }
}
