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
package io.casehub.engine.rest.service;

import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.plan.execution.CasePlanModelSnapshotProvider;
import io.casehub.engine.plan.snapshot.DagPlanSnapshot;
import io.casehub.engine.plan.snapshot.DecompositionSnapshot;
import io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot;
import io.casehub.engine.rest.ExecutionStateBroadcaster;
import io.casehub.engine.rest.exception.EntityNotFoundException;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.PlatformStream;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@McpDomain("engine/plan")
public class DefaultEnginePlanApi {

  @Inject CaseService caseService;
  @Inject CasePlanModelSnapshotProvider planModelProvider;
  @Inject ExecutionSnapshotStore snapshotStore;
  @Inject ExecutionStateBroadcaster executionStateBroadcaster;
  @Inject CurrentPrincipal currentPrincipal;

  @PlatformQuery("Get live case plan model snapshot")
  public Object getPlanModel(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return planModelProvider
        .getSnapshot(caseId, resolvedTenancyId)
        .orElseThrow(() -> new EntityNotFoundException("Plan model not found for case: " + caseId));
  }

  @PlatformQuery("Get plan item definition hierarchy")
  public List<PlanItemDefinitionSnapshot> getPlanDefinitions(
      @PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return planModelProvider.getDefinitions(caseId, resolvedTenancyId);
  }

  @PlatformQuery("Get HTN decomposition tree snapshot")
  public DecompositionSnapshot getDecomposition(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return snapshotStore
        .getDecomposition(caseId, resolvedTenancyId)
        .orElseThrow(
            () -> new EntityNotFoundException("Decomposition not found for case: " + caseId));
  }

  @PlatformQuery("Get DAG plan snapshot")
  public DagPlanSnapshot getDagPlan(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return snapshotStore
        .getDagPlan(caseId, resolvedTenancyId)
        .orElseThrow(() -> new EntityNotFoundException("DAG plan not found for case: " + caseId));
  }

  @PlatformQuery("Get DAG execution result snapshot")
  public Object getDagResult(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return snapshotStore
        .getDagResult(caseId, resolvedTenancyId)
        .orElseThrow(() -> new EntityNotFoundException("DAG result not found for case: " + caseId));
  }

  @PlatformQuery("Get composed execution state snapshot")
  public Object getExecutionState(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return executionStateBroadcaster.composeInitial(caseId, resolvedTenancyId);
  }

  @PlatformStream("Live execution state updates")
  public Multi<Object> executionStateStream(@PathParam UUID caseId) {
    return executionStateBroadcaster.stream(caseId).map(e -> e);
  }
}
