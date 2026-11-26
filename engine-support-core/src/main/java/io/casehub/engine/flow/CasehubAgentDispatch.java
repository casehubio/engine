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
package io.casehub.engine.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.ai.JqTransformer;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.platform.agent.AgentProvider;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.UnaryOperator;
import org.jboss.logging.Logger;

/**
 * Callable dispatcher for {@code casehub:agent} — invokes an eidos agent by ID from within a flow
 * {@code do:} block. First dispatch path that uses identity-based agent lookup rather than slot or
 * capability matching.
 *
 * <p>YAML usage:
 *
 * <pre>{@code
 * do:
 *   - classify:
 *       call: casehub:agent
 *       with:
 *         agentId: classifier-agent
 *         inputProjection: "{ subject: .subject }"
 *         outputProjection: "{ category: .category }"
 *         model: gpt-4o
 * }</pre>
 *
 * <p>Refs casehubio/eidos#279.
 */
public class CasehubAgentDispatch {

  private static final Logger LOG = Logger.getLogger(CasehubAgentDispatch.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final AgentRegistry agentRegistry;
  private final SystemPromptRenderer systemPromptRenderer;
  private final AgentProvider agentProvider;
  private final FlowExecutionRegistry flowExecutionRegistry;
  private final CaseInstanceCache caseInstanceCache;
  private final CallableDispatchRegistry callableDispatchRegistry;
  private final ExecutorService virtualThreads;

  public CasehubAgentDispatch(
      AgentRegistry agentRegistry,
      SystemPromptRenderer systemPromptRenderer,
      AgentProvider agentProvider,
      FlowExecutionRegistry flowExecutionRegistry,
      CaseInstanceCache caseInstanceCache,
      CallableDispatchRegistry callableDispatchRegistry,
      ExecutorService virtualThreads) {
    this.agentRegistry = agentRegistry;
    this.systemPromptRenderer = systemPromptRenderer;
    this.agentProvider = agentProvider;
    this.flowExecutionRegistry = flowExecutionRegistry;
    this.caseInstanceCache = caseInstanceCache;
    this.callableDispatchRegistry = callableDispatchRegistry;
    this.virtualThreads = virtualThreads;
  }

  public void register() {
    callableDispatchRegistry.register("casehub:agent", this::dispatch);
  }

  CompletableFuture<Map<String, Object>> dispatch(
      String workflowInstanceId, Map<String, Object> args) {

    String agentId = (String) args.get("agentId");
    if (agentId == null) {
      throw new IllegalArgumentException(
          "casehub:agent step is missing required 'agentId' argument");
    }

    String inputProjection = (String) args.get("inputProjection");
    String outputProjection = (String) args.get("outputProjection");
    String model = (String) args.get("model");

    FlowExecution execution = flowExecutionRegistry.get(workflowInstanceId);
    CaseInstance instance = caseInstanceCache.get(execution.caseId());
    String tenancyId = instance.tenancyId;

    return CompletableFuture.supplyAsync(
        () -> {
          AgentDescriptor descriptor =
              agentRegistry
                  .findById(agentId, tenancyId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Agent not found: "
                                  + agentId
                                  + " (tenancyId="
                                  + tenancyId
                                  + ")"));

          SystemPromptRenderer.RenderedPrompt rendered =
              systemPromptRenderer.render(
                  descriptor,
                  AgentPromptContext.forFormat(SystemPromptRenderer.RenderFormat.MARKDOWN));

          UnaryOperator<JsonNode> inputTransformer =
              inputProjection != null
                  ? new JqTransformer(inputProjection)::apply
                  : UnaryOperator.identity();
          UnaryOperator<JsonNode> outputTransformer =
              outputProjection != null
                  ? new JqTransformer(outputProjection)::apply
                  : UnaryOperator.identity();

          String effectiveModel = model != null ? model : descriptor.modelFamily();

          WorkerAgentInvoker invoker =
              new WorkerAgentInvoker(
                  agentProvider,
                  rendered.content(),
                  null,
                  inputTransformer,
                  outputTransformer,
                  null,
                  effectiveModel,
                  null);

          JsonNode contextNode = instance.getContextSnapshot();
          @SuppressWarnings("unchecked")
          Map<String, Object> input =
              contextNode != null
                  ? OBJECT_MAPPER.convertValue(contextNode, Map.class)
                  : Map.of();

          LOG.debugf(
              "Dispatching casehub:agent agentId=%s model=%s caseId=%s",
              agentId, effectiveModel, instance.getUuid());

          var response = invoker.executeDetailed(input);
          return response.result().output();
        },
        virtualThreads);
  }
}
