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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.plan.execution.CasePlanModelSnapshotProvider;
import io.casehub.engine.rest.ExecutionStateBroadcaster;
import io.casehub.engine.rest.exception.EntityNotFoundException;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class PlanService {

  @Inject CaseService caseService;
  @Inject CasePlanModelSnapshotProvider planModelProvider;
  @Inject ExecutionSnapshotStore snapshotStore;
  @Inject ExecutionStateBroadcaster executionStateBroadcaster;
  @Inject CurrentPrincipal currentPrincipal;
  @Inject ObjectMapper objectMapper;

  public JsonNode getPlanModel(UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    Object snapshot =
        planModelProvider
            .getSnapshot(caseId, resolvedTenancyId)
            .orElseThrow(
                () -> new EntityNotFoundException("Plan model not found for case: " + caseId));
    return objectMapper.valueToTree(snapshot);
  }

  public JsonNode getPlanDefinitions(UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return objectMapper.valueToTree(planModelProvider.getDefinitions(caseId, resolvedTenancyId));
  }

  public JsonNode getDecomposition(UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    Object decomposition =
        snapshotStore
            .getDecomposition(caseId, resolvedTenancyId)
            .orElseThrow(
                () -> new EntityNotFoundException("Decomposition not found for case: " + caseId));
    return objectMapper.valueToTree(decomposition);
  }

  public JsonNode getDagPlan(UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    Object dagPlan =
        snapshotStore
            .getDagPlan(caseId, resolvedTenancyId)
            .orElseThrow(
                () -> new EntityNotFoundException("DAG plan not found for case: " + caseId));
    return objectMapper.valueToTree(dagPlan);
  }

  public JsonNode getDagResult(UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    Object dagResult =
        snapshotStore
            .getDagResult(caseId, resolvedTenancyId)
            .orElseThrow(
                () -> new EntityNotFoundException("DAG result not found for case: " + caseId));
    return objectMapper.valueToTree(dagResult);
  }

  public JsonNode getExecutionState(UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    Object state = executionStateBroadcaster.composeInitial(caseId, resolvedTenancyId);
    return objectMapper.valueToTree(state);
  }

  public Multi<JsonNode> executionStateStream(UUID caseId) {
    return executionStateBroadcaster.stream(caseId).map(e -> objectMapper.valueToTree(e));
  }
}
