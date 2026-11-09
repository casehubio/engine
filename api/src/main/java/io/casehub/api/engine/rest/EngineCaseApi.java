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
package io.casehub.api.engine.rest;

import io.casehub.api.acl.AclCheck;
import io.casehub.api.acl.EngineResourceTypes;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.view.CaseContextPathView;
import io.casehub.api.view.CaseContextView;
import io.casehub.api.view.CaseInstanceView;
import io.casehub.api.view.CaseOverviewView;
import io.casehub.api.view.CasePage;
import io.casehub.api.view.CaseStreamEventView;
import io.casehub.api.view.CaseWithContextView;
import io.casehub.api.view.GoalEvaluationView;
import io.casehub.api.view.PlanItemView;
import io.casehub.api.view.StartCaseRequest;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.mcp.ContextParam;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PaginatedResponse;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import io.casehub.neocortex.corpus.ChangeSet;
import io.casehub.platform.api.mcp.McpCursorMap;
import io.casehub.platform.api.mcp.McpCursorType;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.PlatformStream;
import io.casehub.platform.api.mcp.RestStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;

@McpDomain(
        value = "engine/cases",
        app = "engine",
        summary = "CMMN case instances — start, query, complete, reactivate")
public interface EngineCaseApi {

    @AclCheck(resourceType = EngineResourceTypes.CASE, action = AclAction.READ)
    @PlatformQuery("List case instances with optional filtering. status: STARTING, RUNNING, WAITING, SUSPENDED, COMPLETED, FAULTED, CANCELLED, COMPENSATING, COMPENSATED, COMPENSATION_FAULTED")
    @PaginatedResponse
    CasePage listCases(
            CaseStatus status,
            String namespace,
            String name,
            @ContextParam("tenancyId") String tenancyId,
            Integer offset,
            Integer limit);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get a case instance by ID")
    CaseInstanceView getCaseById(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get a case instance with its full context in one call — combines getCaseById and getCaseContext")
    CaseWithContextView getCaseWithContext(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get a complete case overview in one call — instance metadata, context, plan items, goals, and the 20 most recent events")
    CaseOverviewView getCaseOverview(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);


    @AclCheck(resourceType = EngineResourceTypes.CASE_DEFINITION, action = AclAction.WRITE)
    @PlatformMutation("Start a new case instance. Request fields: namespace, name, version (identify the definition), context (optional Map of initial case data)")
    @RestStatus(201)
    CaseInstanceView startCase(StartCaseRequest request, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get full case context as JSON")
    CaseContextView getCaseContext(
            @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get case context at a specific path")
    CaseContextPathView getCaseContextPath(
            @PathParam UUID caseId, String path, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get plan items for a case")
    List<PlanItemView> getPlanItems(
            @PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Evaluate goals against live case context")
    GoalEvaluationView getGoals(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);


    @AclCheck(resourceType = EngineResourceTypes.CASE, action = AclAction.READ)
    @PlatformQuery("Cases with activity since an ISO-8601 timestamp — status changes, new events. Returns ChangeSet with case paths (cases/{caseId}) and a newCursor for incremental polling")
    ChangeSet whatChanged(
            @ContextParam("tenancyId") String tenancyId,
            @McpCursorMap(McpCursorType.INSTANT) String since);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformStream("Live case event stream")
    Flow.Publisher<CaseStreamEventView> caseStream(@PathParam UUID caseId);
}
