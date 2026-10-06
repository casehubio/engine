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
import io.casehub.api.engine.rest.EnginePlanApi;
import io.casehub.engine.rest.ExecutionStateBroadcaster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.Flow;

@ApplicationScoped
public class DefaultEnginePlanApi implements EnginePlanApi {

  @Inject PlanService planService;
  @Inject ExecutionStateBroadcaster executionStateBroadcaster;
  @Inject ObjectMapper objectMapper;

  @Override
  public JsonNode getPlanModel(UUID caseId, String tenancyId) {
    return planService.getPlanModel(caseId, tenancyId);
  }

  @Override
  public JsonNode getPlanDefinitions(UUID caseId, String tenancyId) {
    return planService.getPlanDefinitions(caseId, tenancyId);
  }

  @Override
  public JsonNode getDecomposition(UUID caseId, String tenancyId) {
    return planService.getDecomposition(caseId, tenancyId);
  }

  @Override
  public JsonNode getDagPlan(UUID caseId, String tenancyId) {
    return planService.getDagPlan(caseId, tenancyId);
  }

  @Override
  public JsonNode getDagResult(UUID caseId, String tenancyId) {
    return planService.getDagResult(caseId, tenancyId);
  }

  @Override
  public JsonNode getExecutionState(UUID caseId, String tenancyId) {
    return planService.getExecutionState(caseId, tenancyId);
  }

  @Override
  public Flow.Publisher<JsonNode> executionStateStream(UUID caseId) {
    return executionStateBroadcaster.stream(caseId).map(e -> objectMapper.valueToTree(e));
  }
}
