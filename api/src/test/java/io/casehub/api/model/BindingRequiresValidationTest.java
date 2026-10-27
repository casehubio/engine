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

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.worker.api.Capability;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BindingRequiresValidationTest {

  private Binding binding(String name, BindingRequires requires) {
    var b =
        Binding.builder()
            .name(name)
            .capability(Capability.of("cap-" + name, ".", "."))
            .on(new ContextChangeTrigger(".trigger != null"))
            .build();
    b.setRequires(requires);
    return b;
  }

  private Binding binding(String name) {
    return Binding.builder()
        .name(name)
        .capability(Capability.of("cap-" + name, ".", "."))
        .on(new ContextChangeTrigger(".trigger != null"))
        .build();
  }

  @Test
  void noRequiresNoErrors() {
    var errors = Binding.validateRequiresBindings(List.of(binding("a"), binding("b")), List.of());
    assertTrue(errors.isEmpty());
  }

  @Test
  void selfReferenceDetected() {
    var errors =
        Binding.validateRequiresBindings(
            List.of(binding("a", new BindingRequires(List.of("a", "b"), 1, null)), binding("b")),
            List.of());
    assertTrue(errors.stream().anyMatch(e -> e.contains("self-reference")));
  }

  @Test
  void missingBindingDetected() {
    var errors =
        Binding.validateRequiresBindings(
            List.of(
                binding("a", new BindingRequires(List.of("b", "missing"), 1, null)), binding("b")),
            List.of());
    assertTrue(errors.stream().anyMatch(e -> e.contains("missing")));
  }

  @Test
  void cycleDetected() {
    var errors =
        Binding.validateRequiresBindings(
            List.of(
                binding("a", new BindingRequires(List.of("b", "c"), 1, null)),
                binding("b", new BindingRequires(List.of("a", "c"), 1, null)),
                binding("c")),
            List.of());
    assertTrue(errors.stream().anyMatch(e -> e.contains("cycle")));
  }

  @Test
  void duplicateBindingNamesDetected() {
    var errors =
        Binding.validateRequiresBindings(
            List.of(binding("a", new BindingRequires(List.of("b", "b"), 1, null)), binding("b")),
            List.of());
    assertTrue(errors.stream().anyMatch(e -> e.contains("duplicate")));
  }

  @Test
  void scopeMismatchDetected() {
    var compound =
        new CompoundDeclaration(
            "phase1", null, null, Map.of("a", Participation.PARTICIPANT), null, null, false, null);
    var errors =
        Binding.validateRequiresBindings(
            List.of(
                binding("a", new BindingRequires(List.of("b", "c"), 1, null)),
                binding("b"),
                binding("c")),
            List.of(compound));
    assertTrue(errors.stream().anyMatch(e -> e.contains("different scope")));
  }
}
