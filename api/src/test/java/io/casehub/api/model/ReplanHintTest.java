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

import static org.junit.jupiter.api.Assertions.*;

import io.casehub.worker.api.Capability;
import org.junit.jupiter.api.Test;

class ReplanHintTest {

  @Test
  void enumValuesExist() {
    assertEquals(3, ReplanHint.values().length);
    assertNotNull(ReplanHint.ALWAYS);
    assertNotNull(ReplanHint.CONDITIONAL);
    assertNotNull(ReplanHint.NEVER);
  }

  @Test
  void bindingDefaultsToConditional() {
    var binding =
        Binding.builder()
            .name("test-binding")
            .capability(Capability.of("test-cap", ".", "."))
            .on(new ContextChangeTrigger(".ready == true"))
            .build();
    assertEquals(ReplanHint.CONDITIONAL, binding.getReplanHint());
  }

  @Test
  void bindingBuilderSetsReplanHint() {
    var binding =
        Binding.builder()
            .name("test-binding")
            .capability(Capability.of("test-cap", ".", "."))
            .on(new ContextChangeTrigger(".ready == true"))
            .replanHint(ReplanHint.ALWAYS)
            .build();
    assertEquals(ReplanHint.ALWAYS, binding.getReplanHint());
  }

  @Test
  void bindingBuilderSetsNever() {
    var binding =
        Binding.builder()
            .name("test-binding")
            .capability(Capability.of("test-cap", ".", "."))
            .on(new ContextChangeTrigger(".ready == true"))
            .replanHint(ReplanHint.NEVER)
            .build();
    assertEquals(ReplanHint.NEVER, binding.getReplanHint());
  }
}
