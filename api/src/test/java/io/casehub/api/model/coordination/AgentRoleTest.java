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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.casehub.platform.api.path.Path;
import org.junit.jupiter.api.Test;

class AgentRoleTest {

  @Test
  void platform_coordinator_has_initiator_archetype() {
    assertThat(AgentRole.COORDINATOR.archetype()).isEqualTo(RoleArchetype.INITIATOR);
  }

  @Test
  void platform_supervisor_has_initiator_archetype() {
    assertThat(AgentRole.SUPERVISOR.archetype()).isEqualTo(RoleArchetype.INITIATOR);
  }

  @Test
  void platform_judge_has_peer_archetype() {
    assertThat(AgentRole.JUDGE.archetype()).isEqualTo(RoleArchetype.PEER);
  }

  @Test
  void platform_moderator_has_peer_archetype() {
    assertThat(AgentRole.MODERATOR.archetype()).isEqualTo(RoleArchetype.PEER);
  }

  @Test
  void platform_roles_have_correct_platform_role() {
    assertThat(AgentRole.COORDINATOR.platformRole()).isEqualTo(PlatformRole.COORDINATOR);
    assertThat(AgentRole.SUPERVISOR.platformRole()).isEqualTo(PlatformRole.SUPERVISOR);
    assertThat(AgentRole.JUDGE.platformRole()).isEqualTo(PlatformRole.JUDGE);
    assertThat(AgentRole.MODERATOR.platformRole()).isEqualTo(PlatformRole.MODERATOR);
  }

  @Test
  void platform_role_required_archetype_enforced_by_enum() {
    assertThat(PlatformRole.COORDINATOR.requiredArchetype()).isEqualTo(RoleArchetype.INITIATOR);
    assertThat(PlatformRole.SUPERVISOR.requiredArchetype()).isEqualTo(RoleArchetype.INITIATOR);
    assertThat(PlatformRole.JUDGE.requiredArchetype()).isEqualTo(RoleArchetype.PEER);
    assertThat(PlatformRole.MODERATOR.requiredArchetype()).isEqualTo(RoleArchetype.PEER);
  }

  // ── Factory methods ───────────────────────────────────────────────────

  @Test
  void of_with_archetype_creates_domain_role() {
    AgentRole role = AgentRole.of("reviewer/code", RoleArchetype.CONTRACTOR);
    assertThat(role.name()).isEqualTo(Path.parse("reviewer/code"));
    assertThat(role.archetype()).isEqualTo(RoleArchetype.CONTRACTOR);
    assertThat(role.platformRole()).isNull();
  }

  @Test
  void of_symbolic_creates_name_only_role() {
    AgentRole role = AgentRole.of("medic");
    assertThat(role.name()).isEqualTo(Path.parse("medic"));
    assertThat(role.archetype()).isNull();
    assertThat(role.platformRole()).isNull();
  }

  // ── Hierarchy matching ────────────────────────────────────────────────

  @Test
  void isKindOf_self_returns_true() {
    AgentRole reviewer = AgentRole.of("reviewer");
    assertThat(reviewer.isKindOf(reviewer)).isTrue();
  }

  @Test
  void isKindOf_child_matches_parent() {
    AgentRole reviewer = AgentRole.of("reviewer");
    AgentRole codeReviewer = AgentRole.of("reviewer/code");
    assertThat(codeReviewer.isKindOf(reviewer)).isTrue();
  }

  @Test
  void isKindOf_parent_does_not_match_child() {
    AgentRole reviewer = AgentRole.of("reviewer");
    AgentRole codeReviewer = AgentRole.of("reviewer/code");
    assertThat(reviewer.isKindOf(codeReviewer)).isFalse();
  }

  @Test
  void isKindOf_unrelated_returns_false() {
    AgentRole reviewer = AgentRole.of("reviewer/code");
    AgentRole judge = AgentRole.of("judge");
    assertThat(reviewer.isKindOf(judge)).isFalse();
  }

  @Test
  void isKindOf_grandchild_matches_grandparent() {
    AgentRole reviewer = AgentRole.of("reviewer");
    AgentRole securityCodeReviewer = AgentRole.of("reviewer/code/security");
    assertThat(securityCodeReviewer.isKindOf(reviewer)).isTrue();
  }

  // ── Compact constructor invariants ────────────────────────────────────

  @Test
  void null_name_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new AgentRole(null, RoleArchetype.PEER, null));
  }

  @Test
  void platform_role_without_archetype_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AgentRole(Path.parse("custom"), null, PlatformRole.COORDINATOR));
  }

  @Test
  void platform_role_with_wrong_archetype_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new AgentRole(
                    Path.parse("custom"), RoleArchetype.CONTRACTOR, PlatformRole.COORDINATOR));
  }

  @Test
  void platform_role_with_correct_archetype_accepted() {
    AgentRole role =
        new AgentRole(
            Path.parse("custom-coordinator"), RoleArchetype.INITIATOR, PlatformRole.COORDINATOR);
    assertThat(role.platformRole()).isEqualTo(PlatformRole.COORDINATOR);
    assertThat(role.archetype()).isEqualTo(RoleArchetype.INITIATOR);
  }

  @Test
  void path_with_unresolved_delimiter_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new AgentRole(Path.of("reviewer/code"), RoleArchetype.CONTRACTOR, null));
  }

  @Test
  void archetype_without_platform_role_accepted() {
    AgentRole role = new AgentRole(Path.parse("analyst"), RoleArchetype.CONTRACTOR, null);
    assertThat(role.archetype()).isEqualTo(RoleArchetype.CONTRACTOR);
    assertThat(role.platformRole()).isNull();
  }

  @Test
  void symbolic_role_with_null_archetype_and_null_platform_role_accepted() {
    AgentRole role = new AgentRole(Path.parse("observer"), null, null);
    assertThat(role.archetype()).isNull();
    assertThat(role.platformRole()).isNull();
  }

  // ── Record equality ───────────────────────────────────────────────────

  @Test
  void equal_roles_are_equal() {
    AgentRole a = AgentRole.of("reviewer/code", RoleArchetype.CONTRACTOR);
    AgentRole b = AgentRole.of("reviewer/code", RoleArchetype.CONTRACTOR);
    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void different_archetypes_are_not_equal() {
    AgentRole a = AgentRole.of("agent", RoleArchetype.INITIATOR);
    AgentRole b = AgentRole.of("agent", RoleArchetype.CONTRACTOR);
    assertThat(a).isNotEqualTo(b);
  }
}
