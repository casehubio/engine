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

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.view.CoordinationPipelineView;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.platform.api.mcp.ContextParam;
import io.casehub.platform.api.mcp.McpDomain;
import io.casehub.platform.api.mcp.PlatformQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
@McpDomain(
    value = "engine/gallery",
    app = "engine",
    summary = "Gallery projections for agent descriptors and coordination pipelines")
public class GalleryMcpAdapter {

  @Inject CaseDefinitionRegistry definitionRegistry;
  @Inject AgentRegistry agentRegistry;

  @PlatformQuery(
      "Get agent descriptors for all workers in a case definition. Use namespace, name, version from listDefinitions results.")
  public List<AgentDescriptor> getAgentDescriptors(
      String namespace, String name, String version, @ContextParam("tenancyId") String tenancyId) {
    CaseDefinition def = resolveDefinition(namespace, name, version);
    return new AgentDescriptorProjection(agentRegistry).project(def, tenancyId);
  }

  @PlatformQuery(
      "Get coordination pipeline projection for a case definition. Use namespace, name, version from listDefinitions results.")
  public CoordinationPipelineView getCoordinationPipeline(
      String namespace, String name, String version) {
    CaseDefinition def = resolveDefinition(namespace, name, version);
    return new CoordinationPipelineProjection().project(def);
  }

  private CaseDefinition resolveDefinition(String namespace, String name, String version) {
    var meta =
        definitionRegistry
            .findByIdentity(namespace, name, version)
            .orElseThrow(
                () ->
                    new io.casehub.api.engine.EntityNotFoundException(
                        String.format("No definition for %s/%s/%s", namespace, name, version)));
    return definitionRegistry.getCaseDefinition(meta);
  }
}
