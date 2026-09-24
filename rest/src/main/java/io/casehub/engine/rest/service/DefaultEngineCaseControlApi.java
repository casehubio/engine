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
import io.casehub.api.view.CaseControlRequest;
import io.casehub.api.view.CaseControlView;
import io.casehub.api.view.SendSignalRequest;
import io.casehub.api.view.SignalResultView;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
@McpDomain(value = "engine/control", app = "engine", summary = "Control — cancel, resume, send operations")
public class DefaultEngineCaseControlApi {

  @Inject CaseService caseService;
  @Inject CaseHubRuntime runtime;
  @Inject CaseInstanceRepository instanceRepository;
  @Inject CurrentPrincipal currentPrincipal;

  @PlatformMutation("Suspend a running case")
  public CaseControlView suspendCase(
      @PathParam UUID caseId, CaseControlRequest request, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.ADMIN);
    runtime.suspendCase(caseId);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    CaseInstance instance = caseService.requireCase(caseId, resolvedTenancyId);
    return new CaseControlView(caseId, instance.getState());
  }

  @PlatformMutation("Resume a suspended case")
  public CaseControlView resumeCase(
      @PathParam UUID caseId, CaseControlRequest request, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.ADMIN);
    runtime.resumeCase(caseId);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    CaseInstance instance = caseService.requireCase(caseId, resolvedTenancyId);
    return new CaseControlView(caseId, instance.getState());
  }

  @PlatformMutation("Cancel a case")
  public CaseControlView cancelCase(
      @PathParam UUID caseId, CaseControlRequest request, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.ADMIN);
    runtime.cancelCase(caseId);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    CaseInstance instance = caseService.requireCase(caseId, resolvedTenancyId);
    return new CaseControlView(caseId, instance.getState());
  }

  @PlatformMutation("Send a signal to a case")
  public SignalResultView sendSignal(
      @PathParam UUID caseId, SendSignalRequest request, String tenancyId) {
    caseService.requireCaseAccess(caseId, AclAction.WRITE);
    runtime.signal(caseId, request.path(), request.value());
    return new SignalResultView(caseId, true);
  }
}
