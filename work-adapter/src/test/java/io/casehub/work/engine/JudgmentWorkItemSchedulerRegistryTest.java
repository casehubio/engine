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
package io.casehub.work.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.internal.model.PlanItemRecord;
import io.casehub.engine.common.internal.model.PlanItemSaveRequest;
import io.casehub.engine.common.spi.JudgmentScheduleRequest;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.planning.plan.PlanItem;
import io.casehub.engine.planning.registry.BlackboardRegistry;
import io.casehub.work.api.WorkItemCreateRequest;
import io.casehub.work.api.WorkItemRef;
import io.casehub.work.api.spi.WorkItemCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JudgmentWorkItemSchedulerRegistryTest {

  private BlackboardRegistry registry;
  private RecordingWorkItemCreator workItemCreator;
  private RecordingPlanItemStore planItemStore;
  private JudgmentWorkItemScheduler scheduler;

  @BeforeEach
  void setUp() {
    planItemStore = new RecordingPlanItemStore();
    registry = new BlackboardRegistry(planItemStore);
    workItemCreator = new RecordingWorkItemCreator();
    scheduler = new JudgmentWorkItemScheduler();
    scheduler.registry = registry;
    scheduler.workItemCreator = workItemCreator;
    scheduler.planItemStore = planItemStore;
  }

  @Test
  void schedule_shouldNotSilentlyDrop_whenCasePlanModelEvictedBeforeDispatch() {
    UUID caseId = UUID.randomUUID();
    PlanItem planItem =
        PlanItem.create(
            "human-approval",
            io.casehub.api.model.ExecutorRef.of("unused"),
            0,
            JudgmentTarget.builder().title("Approve PR").prompt("review").build());
    assertThat(planItem.tryMarkDispatching()).isTrue();

    registry.getOrCreate(caseId, "test-tenant").addPlanItem(planItem);
    String planItemId = planItem.getPlanItemId();

    registry.evict(caseId);

    JudgmentScheduleRequest request =
        new JudgmentScheduleRequest(
            caseId,
            "test-tenant",
            "human-approval",
            JudgmentTarget.builder().title("Approve PR").prompt("review").build(),
            Map.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            Map.of(),
            planItemId);

    scheduler.schedule(request);

    assertThat(workItemCreator.created).hasSize(1);
    WorkItemCreateRequest created = workItemCreator.created.get(0);
    assertThat(created.callerRef).isEqualTo("case:" + caseId + "/pi:" + planItemId);
    assertThat(created.title).isEqualTo("Approve PR");

    assertThat(planItemStore.saved).hasSize(1);
    PlanItemSaveRequest saved = planItemStore.saved.get(0);
    assertThat(saved.planItemId()).isEqualTo(planItemId);
    assertThat(saved.status()).isEqualTo(TaskStatus.DELEGATED);
  }

  @Test
  void schedule_throwsWhenEvictedAndNoPlanItemId() {
    UUID caseId = UUID.randomUUID();

    JudgmentScheduleRequest request =
        new JudgmentScheduleRequest(
            caseId,
            "test-tenant",
            "human-approval",
            JudgmentTarget.builder().title("Approve PR").prompt("review").build(),
            Map.of(),
            null,
            null);

    assertThatThrownBy(() -> scheduler.schedule(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CasePlanModel evicted")
        .hasMessageContaining("no planItemId");
  }

  @Test
  void schedule_usesRegistryWhenAvailable() {
    UUID caseId = UUID.randomUUID();
    PlanItem planItem =
        PlanItem.create(
            "human-approval",
            io.casehub.api.model.ExecutorRef.of("unused"),
            0,
            JudgmentTarget.builder().title("Approve PR").prompt("review").build());
    assertThat(planItem.tryMarkDispatching()).isTrue();
    registry.getOrCreate(caseId, "test-tenant").addPlanItem(planItem);

    JudgmentScheduleRequest request =
        new JudgmentScheduleRequest(
            caseId,
            "test-tenant",
            "human-approval",
            JudgmentTarget.builder().title("Approve PR").prompt("review").build(),
            Map.of(),
            null,
            null);

    scheduler.schedule(request);

    assertThat(workItemCreator.created).hasSize(1);
    assertThat(planItem.getStatus()).isEqualTo(TaskStatus.DELEGATED);
  }

  static class RecordingWorkItemCreator implements WorkItemCreator {
    final List<WorkItemCreateRequest> created = new ArrayList<>();

    @Override
    public WorkItemRef create(WorkItemCreateRequest request) {
      created.add(request);
      return new WorkItemRef(
          UUID.randomUUID(),
          null,
          request.callerRef,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null);
    }

    @Override
    public Optional<WorkItemRef> findByCallerRef(String callerRef) {
      return Optional.empty();
    }

    @Override
    public Optional<WorkItemRef> findActiveByCallerRef(String callerRef) {
      return Optional.empty();
    }

    @Override
    public void obsoleteByCallerRef(String callerRef) {}
  }

  static class RecordingPlanItemStore implements PlanItemStore {
    final List<PlanItemSaveRequest> saved = new ArrayList<>();

    @Override
    public void save(PlanItemSaveRequest request, String tenancyId) {
      saved.add(request);
    }

    @Override
    public void updateStatus(String planItemId, TaskStatus status, String tenancyId) {}

    @Override
    public List<PlanItemRecord> findByCaseId(UUID caseId, String tenancyId) {
      return List.of();
    }

    @Override
    public List<PlanItemRecord> findDelegated(UUID caseId, String tenancyId) {
      return List.of();
    }
  }
}
