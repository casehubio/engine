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
package io.casehub.api.spi.judgment;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A single piece of typed evidence provided with a judgment response. Carries a name, evidence
 * type, string content, and an optional external reference.
 *
 * <p>Refs engine#1012, engine#1009, engine#994.
 */
public record Evidence(String name, EvidenceType type, String content, @Nullable String ref) {

  public Evidence {
    Objects.requireNonNull(name, "name required");
    Objects.requireNonNull(type, "type required");
    Objects.requireNonNull(content, "content required");
  }

  public static Evidence of(String name, EvidenceType type, String content) {
    return new Evidence(name, type, content, null);
  }
}
