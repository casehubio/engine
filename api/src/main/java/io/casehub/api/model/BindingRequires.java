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

import java.util.List;
import java.util.Objects;

public record BindingRequires(
    List<String> bindings, int atLeast, OnThresholdReached onThresholdReached) {
  public BindingRequires {
    Objects.requireNonNull(bindings, "bindings must not be null");
    if (bindings.size() < 2)
      throw new IllegalArgumentException("requires.bindings must have at least 2 entries");
    if (atLeast < 1 || atLeast > bindings.size())
      throw new IllegalArgumentException(
          "atLeast must be between 1 and bindings.size(), got "
              + atLeast
              + " for "
              + bindings.size()
              + " bindings");
    if (onThresholdReached == null) onThresholdReached = OnThresholdReached.KEEP;
  }
}
