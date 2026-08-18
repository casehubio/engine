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
package io.casehub.engine.plan.goap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GoapWorldStateTest {

  @Test
  void satisfiesAll_all_present() {
    var state = GoapWorldState.closedWorld(Map.of("a", true, "b", true));
    assertThat(state.satisfiesAll(Set.of("a", "b"))).isTrue();
  }

  @Test
  void satisfiesAll_one_missing() {
    var state = GoapWorldState.closedWorld(Map.of("a", true));
    assertThat(state.satisfiesAll(Set.of("a", "b"))).isFalse();
  }

  @Test
  void satisfiesAll_empty_goals() {
    var state = GoapWorldState.closedWorld(Map.of("a", true));
    assertThat(state.satisfiesAll(Set.of())).isTrue();
  }

  @Test
  void get_returns_unknown_for_absent_key() {
    var state = new GoapWorldState(Map.of("a", Condition.TRUE));
    assertThat(state.get("missing")).isEqualTo(Condition.UNKNOWN);
  }

  @Test
  void with_condition_sets_value() {
    var state = new GoapWorldState(Map.of("a", Condition.FALSE));
    var updated = state.with("a", Condition.TRUE);
    assertThat(updated.get("a")).isEqualTo(Condition.TRUE);
  }

  @Test
  void with_boolean_converts_to_condition() {
    var state = new GoapWorldState(Map.of());
    var updated = state.with("x", true);
    assertThat(updated.get("x")).isEqualTo(Condition.TRUE);
  }

  @Test
  void closedWorld_maps_booleans() {
    var state = GoapWorldState.closedWorld(Map.of("a", true, "b", false));
    assertThat(state.get("a")).isEqualTo(Condition.TRUE);
    assertThat(state.get("b")).isEqualTo(Condition.FALSE);
    assertThat(state.get("absent")).isEqualTo(Condition.UNKNOWN);
  }

  @Test
  void satisfies_only_true() {
    var state =
        new GoapWorldState(
            Map.of(
                "yes", Condition.TRUE,
                "no", Condition.FALSE,
                "maybe", Condition.UNKNOWN));
    assertThat(state.satisfies("yes")).isTrue();
    assertThat(state.satisfies("no")).isFalse();
    assertThat(state.satisfies("maybe")).isFalse();
  }

  @Test
  void openWorld_maps_json_keys() {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var json = mapper.createObjectNode();
    json.put("present", "value");
    json.putNull("absent");

    var state = GoapWorldState.openWorld(json);
    assertThat(state.get("present")).isEqualTo(Condition.TRUE);
    assertThat(state.get("absent")).isEqualTo(Condition.UNKNOWN);
    assertThat(state.get("missing")).isEqualTo(Condition.UNKNOWN);
  }
}
