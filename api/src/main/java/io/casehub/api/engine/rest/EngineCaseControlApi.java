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
import io.casehub.api.view.CaseControlRequest;
import io.casehub.api.view.CaseControlView;
import io.casehub.api.view.SendSignalRequest;
import io.casehub.api.view.SignalResultView;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformMutation;
import java.util.UUID;

@McpDomain(
    value = "engine/control",
    app = "engine",
    summary = "Control — cancel, resume, send operations")
public interface EngineCaseControlApi {

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.ADMIN,
      resourceIdParam = "caseId")
  @PlatformMutation("Suspend a running case")
  CaseControlView suspendCase(@PathParam UUID caseId, CaseControlRequest request, String tenancyId);

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.ADMIN,
      resourceIdParam = "caseId")
  @PlatformMutation("Resume a suspended case")
  CaseControlView resumeCase(@PathParam UUID caseId, CaseControlRequest request, String tenancyId);

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.ADMIN,
      resourceIdParam = "caseId")
  @PlatformMutation("Cancel a case")
  CaseControlView cancelCase(@PathParam UUID caseId, CaseControlRequest request, String tenancyId);

  @AclCheck(
      resourceType = EngineResourceTypes.CASE,
      action = AclAction.WRITE,
      resourceIdParam = "caseId")
  @PlatformMutation("Send a signal to a case")
  SignalResultView sendSignal(@PathParam UUID caseId, SendSignalRequest request, String tenancyId);
}
