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

/**
 * Declares what evidence a judgment response must include. Verifiers use these requirements to
 * validate responses.
 *
 * <p>Refs engine#1009, engine#994.
 */
public record EvidenceRequirement(String key, EvidenceType type, boolean required) {

  public EvidenceRequirement(String key, EvidenceType type) {
    this(key, type, true);
  }

  public static EvidenceRequirement required(String key, EvidenceType type) {
    return new EvidenceRequirement(key, type, true);
  }

  public static EvidenceRequirement optional(String key, EvidenceType type) {
    return new EvidenceRequirement(key, type, false);
  }
}
