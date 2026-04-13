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

public enum GoalKind {
  SUCCESS("success"),
  FAILURE("failure");

  private final String value;

  GoalKind(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public static GoalKind fromValue(String value) {
    for (GoalKind kind : values()) {
      if (kind.value.equals(value)) {
        return kind;
      }
    }
    throw new IllegalArgumentException("Unknown GoalKind: " + value);
  }
}
