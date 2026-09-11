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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.spi.routing.ResolutionSourceType;
import org.junit.jupiter.api.Test;

class ResolutionSelectionTest {

  @Test
  void constructsWithRequiredFields() {
    var sel = new ResolutionSelection("case-123", ResolutionSourceType.PLAN_TRACE, null);
    assertThat(sel.selectedCaseId()).isEqualTo("case-123");
    assertThat(sel.sourceType()).isEqualTo(ResolutionSourceType.PLAN_TRACE);
    assertThat(sel.rationale()).isNull();
  }

  @Test
  void constructsWithRationale() {
    var sel =
        new ResolutionSelection(
            "case-456",
            ResolutionSourceType.RESOLUTION_GUIDE,
            "Matches credential harvesting pattern");
    assertThat(sel.rationale()).isEqualTo("Matches credential harvesting pattern");
  }

  @Test
  void rejectsNullCaseId() {
    assertThatThrownBy(() -> new ResolutionSelection(null, ResolutionSourceType.PLAN_TRACE, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsNullSourceType() {
    assertThatThrownBy(() -> new ResolutionSelection("case-123", null, null))
        .isInstanceOf(NullPointerException.class);
  }
}
