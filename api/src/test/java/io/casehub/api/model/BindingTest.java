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

import org.junit.jupiter.api.Test;

class BindingTest {

  @Test
  void builder_withBothCapabilityAndSubCase_throws() {
    Capability cap = Capability.builder().name("c").inputSchema("{}").outputSchema("{}").build();
    SubCase sc = SubCase.builder().namespace("n").name("c").version("1").build();
    assertThatThrownBy(
            () ->
                Binding.builder()
                    .name("b")
                    .capability(cap)
                    .subCase(sc)
                    .on(new ContextChangeTrigger(".x"))
                    .build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("cannot have both");
  }

  @Test
  void builder_withNeitherCapabilityNorSubCase_throws() {
    assertThatThrownBy(() -> Binding.builder().name("b").on(new ContextChangeTrigger(".x")).build())
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void builder_subCaseOnly_valid() {
    SubCase sc = SubCase.builder().namespace("n").name("c").version("1").build();
    Binding b = Binding.builder().name("b").subCase(sc).on(new ContextChangeTrigger(".x")).build();
    assertThat(b.getSubCase()).isNotNull();
    assertThat(b.getCapability()).isNull();
  }

  @Test
  void builder_capabilityOnly_valid() {
    Capability cap = Capability.builder().name("c").inputSchema("{}").outputSchema("{}").build();
    Binding b =
        Binding.builder().name("b").capability(cap).on(new ContextChangeTrigger(".x")).build();
    assertThat(b.getCapability()).isNotNull();
    assertThat(b.getSubCase()).isNull();
  }
}
