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

import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.common.internal.model.PlanItemSaveRequest;
import io.casehub.engine.common.internal.model.TargetType;
import io.casehub.engine.common.spi.JudgmentScheduleRequest;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.persistence.memory.InMemoryPlanItemStore;
import io.casehub.engine.planning.plan.PlanItem;
import io.casehub.engine.planning.registry.BlackboardRegistry;
import io.casehub.work.api.spi.WorkItemCreator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JudgmentWorkItemSchedulerTest {
  private static final String TENANCY_ID = "test-tenant";

  @Inject JudgmentWorkItemScheduler scheduler;
  @Inject BlackboardRegistry registry;
  @Inject WorkItemCreator workItemCreator;
  @Inject PlanItemStore planItemStore;

  private UUID caseId;
  private PlanItem planItem;

  @BeforeEach
  void setUp() {
    if (planItemStore instanceof InMemoryPlanItemStore mem) {
      mem.clear();
    }
    caseId = UUID.randomUUID();
    planItem =
        PlanItem.create(
            "approval-binding", io.casehub.api.model.ExecutorRef.of("unused-worker"), 5);
    assertThat(planItem.tryMarkDispatching()).isTrue();
    registry.getOrCreate(caseId, TENANCY_ID).addPlanItem(planItem);
  }

  @AfterEach
  void tearDown() {
    registry.evict(caseId);
  }

  @Test
  void schedule_createsWorkItem_whenRegistryHasPlanModel() {
    scheduler.schedule(buildRequest());

    assertThat(planItem.getStatus()).isEqualTo(TaskStatus.DELEGATED);
    assertThat(planItemStore.findByCaseId(caseId, TENANCY_ID))
        .anyMatch(
            r ->
                r.planItemId().equals(planItem.getPlanItemId())
                    && r.status() == TaskStatus.DELEGATED);
  }

  @Test
  void schedule_createsWorkItem_whenCasePlanModelEvicted() {
    planItemStore.save(
        PlanItemSaveRequest.primitive(
            caseId,
            planItem.getPlanItemId(),
            planItem.getBindingName(),
            TaskStatus.DISPATCHING,
            planItem.getCreatedAt(),
            TargetType.JUDGMENT,
            null,
            TENANCY_ID,
            null,
            null,
            null),
        TENANCY_ID);

    registry.evict(caseId);

    scheduler.schedule(buildRequest());

    assertThat(planItemStore.findByCaseId(caseId, TENANCY_ID))
        .anyMatch(
            r ->
                r.planItemId().equals(planItem.getPlanItemId())
                    && r.status() == TaskStatus.DELEGATED);
  }

  @SuppressWarnings("deprecation")
  private JudgmentScheduleRequest buildRequest() {
    JudgmentTarget target =
        JudgmentTarget.builder().title("Approve PR").prompt("Review this PR").build();
    return new JudgmentScheduleRequest(
        caseId, TENANCY_ID, "approval-binding", target, Map.of(), null, null);
  }
}
