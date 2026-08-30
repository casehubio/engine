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

import org.jspecify.annotations.Nullable;

/**
 * Identity of the caller who responded to a judgment request. Wraps the raw callerId and callerType
 * strings into a structured record.
 *
 * <p>Refs engine#1009, engine#994.
 */
public record CallerIdentity(@Nullable String callerId, @Nullable String callerType) {

  public static CallerIdentity of(@Nullable String callerId, @Nullable String callerType) {
    return new CallerIdentity(callerId, callerType);
  }

  public static CallerIdentity anonymous() {
    return new CallerIdentity(null, null);
  }
}
