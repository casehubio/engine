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
import io.casehub.api.view.CaseDefinitionPage;
import io.casehub.api.view.CaseDefinitionView;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PaginatedResponse;
import io.casehub.platform.api.mcp.PathParam;
import io.casehub.platform.api.mcp.PlatformQuery;
import java.util.List;

@McpDomain(
    value = "engine/definitions",
    app = "engine",
    summary =
        "List registered case definitions; Get definitions by namespace and name; Get a specific definition by namespace, name, and version")
public interface EngineCaseDefinitionApi {

  @AclCheck(resourceType = EngineResourceTypes.CASE_DEFINITION, action = AclAction.READ)
  @PlatformQuery("List registered case definitions")
  @PaginatedResponse
  CaseDefinitionPage listDefinitions(String tenancyId, Integer offset, Integer limit);

  @AclCheck(resourceType = EngineResourceTypes.CASE_DEFINITION, action = AclAction.READ)
  @PlatformQuery("Get definitions by namespace and name")
  List<CaseDefinitionView> getDefinitionsByName(
      @PathParam String namespace, @PathParam String name, String tenancyId);

  @AclCheck(resourceType = EngineResourceTypes.CASE_DEFINITION, action = AclAction.READ)
  @PlatformQuery("Get a specific definition by namespace, name, and version")
  CaseDefinitionView getDefinitionByKey(
      @PathParam String namespace,
      @PathParam String name,
      @PathParam String version,
      String tenancyId);
}
