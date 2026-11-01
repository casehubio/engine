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
package io.casehub.api.model.coordination;

import io.casehub.platform.api.path.Path;
import jakarta.annotation.Nullable;
import java.util.Objects;

public record AgentRole(
    Path name, @Nullable RoleArchetype archetype, @Nullable PlatformRole platformRole) {

  public static final AgentRole COORDINATOR =
      new AgentRole(Path.parse("coordinator"), RoleArchetype.INITIATOR, PlatformRole.COORDINATOR);

  public static final AgentRole SUPERVISOR =
      new AgentRole(Path.parse("supervisor"), RoleArchetype.INITIATOR, PlatformRole.SUPERVISOR);

  public static final AgentRole JUDGE =
      new AgentRole(Path.parse("judge"), RoleArchetype.PEER, PlatformRole.JUDGE);

  public static final AgentRole MODERATOR =
      new AgentRole(Path.parse("moderator"), RoleArchetype.PEER, PlatformRole.MODERATOR);

  public AgentRole {
    Objects.requireNonNull(name, "name must not be null");
    for (final String segment : name.segments()) {
      if (segment.contains("/")) {
        throw new IllegalArgumentException(
            "AgentRole name must be constructed via Path.parse() — "
                + "segment contains unresolved delimiter: \""
                + segment
                + "\"");
      }
    }
    if (platformRole != null && archetype == null) {
      throw new IllegalArgumentException(
          "platformRole requires archetype — a behavioral contract implies structural position");
    }
    if (platformRole != null && archetype != platformRole.requiredArchetype()) {
      throw new IllegalArgumentException(
          platformRole
              + " requires archetype "
              + platformRole.requiredArchetype()
              + ", got "
              + archetype);
    }
  }

  public static AgentRole of(final String name, final RoleArchetype archetype) {
    return new AgentRole(Path.parse(name), archetype, null);
  }

  public static AgentRole of(final String name) {
    return new AgentRole(Path.parse(name), null, null);
  }

  public boolean isKindOf(final AgentRole other) {
    return this.name.equals(other.name) || other.name.isAncestorOf(this.name);
  }
}
