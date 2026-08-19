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
package io.casehub.engine.plan.monitoring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpectedEffectsTest {

  @Test
  void isEmpty_when_no_effects() {
    var effects = new ExpectedEffects(Map.of(), ExpectedEffects.EffectSource.GOAP);
    assertTrue(effects.isEmpty());
  }

  @Test
  void not_empty_with_effects() {
    var effects = new ExpectedEffects(Map.of("resolved", true), ExpectedEffects.EffectSource.GOAP);
    assertFalse(effects.isEmpty());
  }

  @Test
  void null_effects_become_empty_map() {
    var effects = new ExpectedEffects(null, ExpectedEffects.EffectSource.PRODUCED_KEYS);
    assertTrue(effects.isEmpty());
    assertNotNull(effects.effects());
  }

  @Test
  void effects_are_immutable() {
    var effects = new ExpectedEffects(Map.of("key", true), ExpectedEffects.EffectSource.GOAP);
    assertThrows(UnsupportedOperationException.class, () -> effects.effects().put("new", false));
  }

  @Test
  void source_is_required() {
    assertThrows(NullPointerException.class, () -> new ExpectedEffects(Map.of(), null));
  }
}
