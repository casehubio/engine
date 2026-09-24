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

import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.view.CaseContextChangeEventView;
import io.casehub.api.view.CaseInstanceView;
import io.casehub.api.view.CaseLifecycleEventView;
import io.casehub.api.view.CasePage;
import io.casehub.api.view.CaseStreamEventView;
import io.casehub.api.view.GoalEvaluationView;
import io.casehub.api.view.PlanItemView;
import io.casehub.api.view.StartCaseRequest;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.internal.model.PlanItemRecord;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.query.CaseInstanceQuery;
import io.casehub.engine.rest.CaseStreamBroadcaster;
import io.casehub.platform.api.acl.AccessControlProvider;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.acl.ResourceId;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PaginatedResponse;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.PlatformStream;
import io.casehub.platform.api.mcp.RestStatus;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@McpDomain(value = "engine/cases", app = "engine", summary = "CMMN case instances — start, query, complete, reactivate")
public class DefaultEngineCaseApi {

  @Inject CaseService caseService;
  @Inject CaseHubRuntime runtime;
  @Inject CaseInstanceRepository instanceRepository;
  @Inject PlanItemStore planItemStore;
  @Inject CurrentPrincipal currentPrincipal;
  @Inject AccessControlProvider accessControlProvider;
  @Inject CaseStreamBroadcaster caseStreamBroadcaster;

  @PlatformQuery("List case instances with optional filtering")
  @PaginatedResponse
  public CasePage listCases(
      CaseStatus status,
      String namespace,
      String name,
      String tenancyId,
      Integer offset,
      Integer limit) {
    int page = offset != null ? offset : 0;
    int size = limit != null ? limit : 20;
    var query =
        CaseInstanceQuery.builder()
            .status(status)
            .namespace(namespace)
            .name(name)
            .page(page)
            .size(size)
            .build();
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    String actorId = currentPrincipal.actorId();
    var items =
        instanceRepository.query(query, resolvedTenancyId).stream()
            .filter(
                ci ->
                    accessControlProvider.canAccess(
                        actorId,
                        new ResourceId(
                            io.casehub.api.acl.EngineResourceTypes.CASE, ci.getUuid().toString()),
                        AclAction.READ))
            .map(this::mapInstance)
            .toList();
    long total = items.size();
    return new CasePage(items, total, total > (long) page * size + size);
  }

  @PlatformQuery("Get a case instance by ID")
  public CaseInstanceView getCaseById(@PathParam UUID caseId, String tenancyId) {
    var instance = caseService.requireCaseAccess(caseId, AclAction.READ);
    return mapInstance(instance);
  }

  @PlatformMutation("Start a new case instance")
  @RestStatus(201)
  public CaseInstanceView startCase(StartCaseRequest request, String tenancyId) {
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    var instance =
        caseService.startCase(
            request.namespace(),
            request.name(),
            request.version(),
            request.context(),
            resolvedTenancyId);
    return mapInstance(instance);
  }

  @SuppressWarnings("unchecked")
  @PlatformQuery("Get full case context as JSON")
  public Map<String, Object> getCaseContext(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    Object context = runtime.query(caseId, ".");
    return context instanceof Map ? (Map<String, Object>) context : Map.of();
  }

  @SuppressWarnings("unchecked")
  @PlatformQuery("Get case context at a specific path")
  public Map<String, Object> getCaseContextPath(
      @PathParam UUID caseId, String path, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    Object value = runtime.query(caseId, path);
    return value instanceof Map ? (Map<String, Object>) value : Map.of();
  }

  @PlatformQuery("Get plan items for a case")
  public List<PlanItemView> getPlanItems(@PathParam UUID caseId, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return planItemStore.findByCaseId(caseId, resolvedTenancyId).stream()
        .map(this::mapPlanItem)
        .toList();
  }

  @PlatformQuery("Evaluate goals against live case context")
  public GoalEvaluationView getGoals(@PathParam UUID caseId, String tenancyId) {
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return caseService.evaluateGoals(caseId, resolvedTenancyId);
  }

  @PlatformStream("Live case event stream")
  public Multi<CaseStreamEventView> caseStream(@PathParam UUID caseId) {
    return caseStreamBroadcaster.stream(caseId);
  }

  @PlatformStream("Live case lifecycle events")
  public Multi<CaseLifecycleEventView> caseLifecycle(@PathParam UUID caseId) {
    return Multi.createFrom().empty();
  }

  @PlatformStream("Live case context change events")
  public Multi<CaseContextChangeEventView> caseContextChange(@PathParam UUID caseId) {
    return Multi.createFrom().empty();
  }

  private CaseInstanceView mapInstance(CaseInstance instance) {
    CaseMetaModel meta = instance.getCaseMetaModel();
    return new CaseInstanceView(
        instance.getUuid(),
        instance.getState(),
        meta.getNamespace(),
        meta.getName(),
        meta.getVersion(),
        instance.getCreatedAt(),
        instance.getActorId());
  }

  private PlanItemView mapPlanItem(PlanItemRecord record) {
    return new PlanItemView(
        UUID.fromString(record.planItemId()),
        record.bindingName(),
        record.status(),
        record.targetType().name().toLowerCase().replace('_', '-'),
        record.parentCompoundId() != null ? UUID.fromString(record.parentCompoundId()) : null);
  }
}
