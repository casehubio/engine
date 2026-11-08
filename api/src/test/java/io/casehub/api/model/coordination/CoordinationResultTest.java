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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoordinationResultTest {

  @Test
  void sealed_interface_permits_three_variants() {
    assertThat(CoordinationResult.class.getPermittedSubclasses())
        .hasSize(3)
        .extracting(Class::getSimpleName)
        .containsExactlyInAnyOrder("Aggregated", "Partial", "Failed");
  }

  @Test
  void aggregated_valid_construction() {
    var result = new CoordinationResult.Aggregated("merged output", List.of("a-1", "a-2"));
    assertThat(result.payload()).isEqualTo("merged output");
    assertThat(result.contributors()).containsExactly("a-1", "a-2");
  }

  @Test
  void aggregated_null_payload_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationResult.Aggregated(null, List.of()))
        .withMessageContaining("payload");
  }

  @Test
  void aggregated_defensive_copy() {
    var list = new ArrayList<>(List.of("a-1"));
    var result = new CoordinationResult.Aggregated("output", list);
    list.add("a-2");
    assertThat(result.contributors()).hasSize(1);
  }

  @Test
  void partial_valid_construction() {
    var result = new CoordinationResult.Partial("interim", List.of("a-1"), List.of("a-2"));
    assertThat(result.payload()).isEqualTo("interim");
    assertThat(result.completed()).containsExactly("a-1");
    assertThat(result.pending()).containsExactly("a-2");
  }

  @Test
  void failed_valid_construction() {
    var result = new CoordinationResult.Failed("timeout", List.of("a-3"));
    assertThat(result.reason()).isEqualTo("timeout");
    assertThat(result.failedAgents()).containsExactly("a-3");
  }

  @Test
  void failed_null_reason_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationResult.Failed(null, List.of()))
        .withMessageContaining("reason");
  }
}
