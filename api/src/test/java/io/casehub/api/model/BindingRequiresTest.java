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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BindingRequiresTest {

  @Test
  void validRequires() {
    var req = new BindingRequires(List.of("a", "b", "c"), 2, OnThresholdReached.KEEP);
    assertEquals(3, req.bindings().size());
    assertEquals(2, req.atLeast());
    assertEquals(OnThresholdReached.KEEP, req.onThresholdReached());
  }

  @Test
  void defaultsToKeep() {
    var req = new BindingRequires(List.of("a", "b"), 1, null);
    assertEquals(OnThresholdReached.KEEP, req.onThresholdReached());
  }

  @Test
  void rejectsTooFewBindings() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BindingRequires(List.of("a"), 1, OnThresholdReached.KEEP));
  }

  @Test
  void rejectsAtLeastGreaterThanBindingsSize() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BindingRequires(List.of("a", "b"), 3, OnThresholdReached.KEEP));
  }

  @Test
  void rejectsAtLeastZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new BindingRequires(List.of("a", "b"), 0, OnThresholdReached.KEEP));
  }

  @Test
  void rejectsNullBindings() {
    assertThrows(
        NullPointerException.class, () -> new BindingRequires(null, 1, OnThresholdReached.KEEP));
  }
}
