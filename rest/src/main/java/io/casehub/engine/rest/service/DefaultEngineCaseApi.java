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

import io.casehub.api.engine.rest.EngineCaseApi;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.view.CaseContextPathView;
import io.casehub.api.view.CaseContextView;
import io.casehub.api.view.CaseInstanceView;
import io.casehub.api.view.CasePage;
import io.casehub.api.view.CaseStreamEventView;
import io.casehub.api.view.GoalEvaluationView;
import io.casehub.api.view.PlanItemView;
import io.casehub.api.view.StartCaseRequest;
import io.casehub.engine.rest.CaseStreamBroadcaster;
import io.casehub.platform.api.acl.AclAction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;

@ApplicationScoped
public class DefaultEngineCaseApi implements EngineCaseApi {

  @Inject CaseService caseService;
  @Inject CaseStreamBroadcaster caseStreamBroadcaster;

  @Override
  public CasePage listCases(
      CaseStatus status,
      String namespace,
      String name,
      String tenancyId,
      Integer offset,
      Integer limit) {
    return caseService.listCases(status, namespace, name, tenancyId, offset, limit);
  }

  @Override
  public CaseInstanceView getCaseById(UUID caseId, String tenancyId) {
    var instance = caseService.requireCaseAccess(caseId, AclAction.READ);
    return caseService.mapInstance(instance);
  }

  @Override
  public CaseInstanceView startCase(StartCaseRequest request, String tenancyId) {
    return caseService.startCaseAndMap(request, tenancyId);
  }

  @Override
  public CaseContextView getCaseContext(UUID caseId, String tenancyId) {
    return caseService.getCaseContext(caseId, tenancyId);
  }

  @Override
  public CaseContextPathView getCaseContextPath(UUID caseId, String path, String tenancyId) {
    return caseService.getCaseContextPath(caseId, path, tenancyId);
  }

  @Override
  public List<PlanItemView> getPlanItems(UUID caseId, String tenancyId) {
    return caseService.getPlanItems(caseId, tenancyId);
  }

  @Override
  public GoalEvaluationView getGoals(UUID caseId, String tenancyId) {
    return caseService.evaluateGoals(caseId, tenancyId);
  }

  @Override
  public Flow.Publisher<CaseStreamEventView> caseStream(UUID caseId) {
    return caseStreamBroadcaster.stream(caseId);
  }
}
