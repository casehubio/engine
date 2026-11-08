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
import io.casehub.engine.plan.snapshot.DagPlanSnapshot;
import io.casehub.engine.plan.snapshot.DagResultView;
import io.casehub.engine.plan.snapshot.DecompositionSnapshot;
import io.casehub.engine.plan.snapshot.ExecutionStateView;
import io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot;
import io.casehub.engine.plan.snapshot.PlanModelView;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.mcp.ContextParam;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformQuery;
import io.casehub.platform.api.mcp.PlatformStream;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;

@McpDomain(value = "engine/plan", app = "engine", summary = "Plan — get, live operations")
public interface EnginePlanApi {

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get live case plan model snapshot with agenda, focus, sub-cases, and compound status")
    PlanModelView getPlanModel(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get plan item definition hierarchy as a typed tree of primitive and compound items")
    List<PlanItemDefinitionSnapshot> getPlanDefinitions(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get HTN decomposition tree snapshot as a typed tree of leaf and compound tasks")
    DecompositionSnapshot getDecomposition(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get DAG plan snapshot as typed nodes with dependencies and join types")
    DagPlanSnapshot getDagPlan(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get DAG execution result snapshot with node states, completion status, and timing")
    DagResultView getDagResult(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformQuery("Get composed execution state: model, active/completed agents, iteration, timing")
    ExecutionStateView getExecutionState(@PathParam UUID caseId, @ContextParam("tenancyId") String tenancyId);

    @AclCheck(
            resourceType = EngineResourceTypes.CASE,
            action = AclAction.READ,
            resourceIdParam = "caseId")
    @PlatformStream("Live execution state updates")
    Flow.Publisher<ExecutionStateView> executionStateStream(@PathParam UUID caseId);
}
