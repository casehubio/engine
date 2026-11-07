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
package io.casehub.api.model.coordination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import io.casehub.api.model.stigmergy.BehavioralFingerprint;
import io.casehub.api.model.stigmergy.DetectedRole;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleSuggestionTest {

  static final DetectedRole SOURCE =
      new DetectedRole(
          "cluster-1",
          Set.of("agent-a"),
          BehavioralFingerprint.EMPTY,
          List.of("coordination"),
          5,
          0.1);

  @Test
  void valid_suggestion_with_all_fields() {
    var suggestion =
        new RoleSuggestion(
            "coordinator", StructuralRole.INITIATOR, PlatformRole.COORDINATOR, 0.85, SOURCE);
    assertThat(suggestion.suggestedName()).isEqualTo("coordinator");
    assertThat(suggestion.suggestedStructuralRole()).isEqualTo(StructuralRole.INITIATOR);
    assertThat(suggestion.suggestedPlatformRole()).isEqualTo(PlatformRole.COORDINATOR);
    assertThat(suggestion.confidence()).isEqualTo(0.85);
    assertThat(suggestion.source()).isEqualTo(SOURCE);
  }

  @Test
  void null_structural_role_accepted() {
    assertThatNoException()
        .isThrownBy(() -> new RoleSuggestion("analyst", null, null, 0.7, SOURCE));
  }

  @Test
  void null_platform_role_accepted() {
    assertThatNoException()
        .isThrownBy(
            () -> new RoleSuggestion("worker", StructuralRole.CONTRACTOR, null, 0.6, SOURCE));
  }

  @Test
  void null_platform_with_null_structural_accepted() {
    var suggestion = new RoleSuggestion("observer", null, null, 0.5, SOURCE);
    assertThat(suggestion.suggestedStructuralRole()).isNull();
    assertThat(suggestion.suggestedPlatformRole()).isNull();
  }

  @Test
  void confidence_zero_accepted() {
    assertThatNoException().isThrownBy(() -> new RoleSuggestion("low", null, null, 0.0, SOURCE));
  }

  @Test
  void confidence_one_accepted() {
    assertThatNoException().isThrownBy(() -> new RoleSuggestion("high", null, null, 1.0, SOURCE));
  }

  @Test
  void confidence_below_zero_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RoleSuggestion("bad", null, null, -0.01, SOURCE))
        .withMessageContaining("confidence");
  }

  @Test
  void confidence_above_one_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RoleSuggestion("bad", null, null, 1.01, SOURCE))
        .withMessageContaining("confidence");
  }

  @Test
  void inconsistent_platform_structural_pairing_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new RoleSuggestion(
                    "bad", StructuralRole.CONTRACTOR, PlatformRole.COORDINATOR, 0.5, SOURCE))
        .withMessageContaining("COORDINATOR")
        .withMessageContaining("INITIATOR");
  }

  @Test
  void consistent_platform_structural_pairing_accepted() {
    assertThatNoException()
        .isThrownBy(
            () ->
                new RoleSuggestion("judge", StructuralRole.PEER, PlatformRole.JUDGE, 0.9, SOURCE));
  }

  @Test
  void platform_role_with_null_structural_accepted() {
    var suggestion = new RoleSuggestion("coordinator", null, PlatformRole.COORDINATOR, 0.8, SOURCE);
    assertThat(suggestion.suggestedPlatformRole()).isEqualTo(PlatformRole.COORDINATOR);
    assertThat(suggestion.suggestedStructuralRole()).isNull();
  }
}
