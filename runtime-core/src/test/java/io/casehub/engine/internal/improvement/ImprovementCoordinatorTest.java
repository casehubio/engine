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
package io.casehub.engine.internal.improvement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImprovementCoordinatorTest {

  private ImprovementCoordinator coordinator;
  private UUID caseId;

  @BeforeEach
  void setUp() {
    coordinator = new ImprovementCoordinator();
    caseId = UUID.randomUUID();
  }

  @Test
  void blockAndCheckBlocked() {
    var improvementId = UUID.randomUUID();
    var blockerId = UUID.randomUUID();
    coordinator.block(caseId, improvementId, blockerId);

    assertThat(coordinator.isBlocked(caseId, improvementId)).isTrue();
  }

  @Test
  void unblockReleasesBlock() {
    var improvementId = UUID.randomUUID();
    var blockerId = UUID.randomUUID();
    coordinator.block(caseId, improvementId, blockerId);
    coordinator.unblock(caseId, improvementId);

    assertThat(coordinator.isBlocked(caseId, improvementId)).isFalse();
  }

  @Test
  void unblockedByDefault() {
    assertThat(coordinator.isBlocked(caseId, UUID.randomUUID())).isFalse();
  }

  @Test
  void blockedByReturnsBlocker() {
    var improvementId = UUID.randomUUID();
    var blockerId = UUID.randomUUID();
    coordinator.block(caseId, improvementId, blockerId);

    assertThat(coordinator.blockedBy(caseId, improvementId)).isEqualTo(blockerId);
  }

  @Test
  void blockedByReturnsNullWhenNotBlocked() {
    assertThat(coordinator.blockedBy(caseId, UUID.randomUUID())).isNull();
  }

  @Test
  void perCaseIsolation() {
    var case2 = UUID.randomUUID();
    var improvementId = UUID.randomUUID();
    var blockerId = UUID.randomUUID();
    coordinator.block(caseId, improvementId, blockerId);

    assertThat(coordinator.isBlocked(caseId, improvementId)).isTrue();
    assertThat(coordinator.isBlocked(case2, improvementId)).isFalse();
  }

  @Test
  void resetClearsAllBlocks() {
    var improvementId = UUID.randomUUID();
    coordinator.block(caseId, improvementId, UUID.randomUUID());
    coordinator.reset();

    assertThat(coordinator.isBlocked(caseId, improvementId)).isFalse();
  }
}
