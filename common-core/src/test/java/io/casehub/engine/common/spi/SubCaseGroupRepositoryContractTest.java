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
package io.casehub.engine.common.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.OnThresholdReached;
import io.casehub.engine.common.internal.model.SubCaseGroup;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public abstract class SubCaseGroupRepositoryContractTest {

  protected abstract SubCaseGroupRepository repository();

  protected abstract String tenancyId();

  private final UUID parentCaseId = UUID.randomUUID();

  @Test
  void getOrCreate_createsNewGroup() {
    SubCaseGroup group =
        repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());

    assertThat(group.getParentCaseId()).isEqualTo(parentCaseId);
    assertThat(group.getGroupId()).isEqualTo("g1");
    assertThat(group.getInstanceCount()).isEqualTo(3);
    assertThat(group.getRequiredCount()).isEqualTo(2);
    assertThat(group.getCompletedCount()).isZero();
    assertThat(group.getRejectedCount()).isZero();
    assertThat(group.isPolicyTriggered()).isFalse();
  }

  @Test
  void getOrCreate_returnsExistingGroup() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());
    SubCaseGroup second =
        repository().getOrCreate(parentCaseId, "g1", 5, 4, OnThresholdReached.CANCEL, tenancyId());

    assertThat(second.getInstanceCount()).isEqualTo(3);
    assertThat(second.getRequiredCount()).isEqualTo(2);
  }

  @Test
  void registerChild_addsChildToGroup() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());
    UUID childId = UUID.randomUUID();

    SubCaseGroup group = repository().registerChild(parentCaseId, "g1", childId, tenancyId());

    assertThat(group.getChildCaseIds()).containsExactly(childId);
  }

  @Test
  void registerChild_throwsForUnknownGroup() {
    assertThatThrownBy(
            () ->
                repository()
                    .registerChild(UUID.randomUUID(), "unknown", UUID.randomUUID(), tenancyId()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void incrementCompleted_incrementsCount() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());

    repository().incrementCompleted(parentCaseId, "g1", tenancyId());
    SubCaseGroup group = repository().incrementCompleted(parentCaseId, "g1", tenancyId());

    assertThat(group.getCompletedCount()).isEqualTo(2);
  }

  @Test
  void incrementRejected_incrementsCount() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());

    SubCaseGroup group = repository().incrementRejected(parentCaseId, "g1", tenancyId());

    assertThat(group.getRejectedCount()).isEqualTo(1);
  }

  @Test
  void markPolicyTriggered_returnsTrueFirstTime() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());

    assertThat(repository().markPolicyTriggered(parentCaseId, "g1", tenancyId())).isTrue();
  }

  @Test
  void markPolicyTriggered_returnsFalseSecondTime() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());

    repository().markPolicyTriggered(parentCaseId, "g1", tenancyId());
    assertThat(repository().markPolicyTriggered(parentCaseId, "g1", tenancyId())).isFalse();
  }

  @Test
  void markPolicyTriggered_returnsFalseForUnknownGroup() {
    assertThat(repository().markPolicyTriggered(UUID.randomUUID(), "unknown", tenancyId()))
        .isFalse();
  }

  @Test
  void findByChildCaseId_findsRegisteredChild() {
    repository().getOrCreate(parentCaseId, "g1", 3, 2, OnThresholdReached.KEEP, tenancyId());
    UUID childId = UUID.randomUUID();
    repository().registerChild(parentCaseId, "g1", childId, tenancyId());

    assertThat(repository().findByChildCaseId(childId, tenancyId()))
        .isPresent()
        .get()
        .extracting(SubCaseGroup::getParentCaseId)
        .isEqualTo(parentCaseId);
  }

  @Test
  void findByChildCaseId_returnsEmptyForUnknownChild() {
    assertThat(repository().findByChildCaseId(UUID.randomUUID(), tenancyId())).isEmpty();
  }
}
