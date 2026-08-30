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
 * Identity of the caller who responded to a judgment request. Both callerId and callerType are
 * required — if no caller identity is known, the field is null at the container level.
 *
 * <p>Refs engine#1012, engine#1009, engine#994.
 */
public record CallerIdentity(String callerId, String callerType, @Nullable Double trustScore) {

  public CallerIdentity {
    Objects.requireNonNull(callerId, "callerId required");
    Objects.requireNonNull(callerType, "callerType required");
  }

  public static CallerIdentity of(String callerId, String callerType) {
    return new CallerIdentity(callerId, callerType, null);
  }

  public static CallerIdentity of(String callerId, String callerType, @Nullable Double trustScore) {
    return new CallerIdentity(callerId, callerType, trustScore);
  }
}
