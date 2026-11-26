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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentPromptContext;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.SystemPromptRenderer;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.platform.agent.AgentProvider;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasehubAgentDispatchTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String TENANT = "test-tenant";
  private static final String AGENT_ID = "classifier-agent";
  private static final UUID CASE_ID = UUID.randomUUID();
  private static final String WORKFLOW_ID = "wf-1";

  CasehubAgentDispatch dispatch;
  AgentRegistry agentRegistry;
  SystemPromptRenderer systemPromptRenderer;
  AgentProvider agentProvider;
  FlowExecutionRegistry flowExecutionRegistry;
  CaseInstanceCache caseInstanceCache;
  CallableDispatchRegistry callableDispatchRegistry;

  @BeforeEach
  void setUp() {
    agentRegistry = mock(AgentRegistry.class);
    systemPromptRenderer = mock(SystemPromptRenderer.class);
    agentProvider = mock(AgentProvider.class);
    flowExecutionRegistry = mock(FlowExecutionRegistry.class);
    caseInstanceCache = mock(CaseInstanceCache.class);
    callableDispatchRegistry = new CallableDispatchRegistry();

    dispatch =
        new CasehubAgentDispatch(
            agentRegistry,
            systemPromptRenderer,
            agentProvider,
            flowExecutionRegistry,
            caseInstanceCache,
            callableDispatchRegistry,
            Executors.newVirtualThreadPerTaskExecutor());
  }

  @Test
  void register_addsToRegistry() {
    dispatch.register();
    assertThat(callableDispatchRegistry.canHandle("casehub:agent")).isTrue();
  }

  @Test
  void dispatch_missingAgentId_throws() {
    assertThatThrownBy(() -> dispatch.dispatch(WORKFLOW_ID, Map.of()).get())
        .hasCauseInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("agentId");
  }

  @Test
  void dispatch_agentNotFound_throws() {
    stubFlowContext();
    when(agentRegistry.findById(AGENT_ID, TENANT)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> dispatch.dispatch(WORKFLOW_ID, Map.of("agentId", AGENT_ID)).get())
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Agent not found: classifier-agent");
  }

  @Test
  void dispatch_agentFound_callsRegistryWithCorrectTenancy() throws Exception {
    stubFlowContext();
    AgentDescriptor descriptor = minimalAgent(AGENT_ID);
    when(agentRegistry.findById(AGENT_ID, TENANT)).thenReturn(Optional.of(descriptor));
    when(systemPromptRenderer.render(eq(descriptor), any(AgentPromptContext.class)))
        .thenReturn(
            new SystemPromptRenderer.RenderedPrompt(
                "You are a classifier.",
                SystemPromptRenderer.RenderFormat.MARKDOWN,
                "",
                "",
                false));

    // Full invocation requires mocking AgentProvider.invoke() → Multi<AgentEvent>, which is
    // an integration-level concern. Verify the dispatch path resolves agent and renders prompt
    // by catching the AgentException from the unmocked provider.
    assertThatThrownBy(() -> dispatch.dispatch(WORKFLOW_ID, Map.of("agentId", AGENT_ID)).get())
        .hasCauseInstanceOf(Exception.class);

    org.mockito.Mockito.verify(agentRegistry).findById(AGENT_ID, TENANT);
    org.mockito.Mockito.verify(systemPromptRenderer).render(eq(descriptor), any());
  }

  @Test
  void register_duplicateThrows() {
    dispatch.register();
    CasehubAgentDispatch second =
        new CasehubAgentDispatch(
            agentRegistry,
            systemPromptRenderer,
            agentProvider,
            flowExecutionRegistry,
            caseInstanceCache,
            callableDispatchRegistry,
            Executors.newVirtualThreadPerTaskExecutor());
    assertThatThrownBy(second::register)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("casehub:agent");
  }

  private void stubFlowContext() {
    FlowExecution execution = mock(FlowExecution.class);
    when(execution.caseId()).thenReturn(CASE_ID);
    when(flowExecutionRegistry.get(WORKFLOW_ID)).thenReturn(execution);

    CaseInstance instance = new CaseInstance();
    instance.tenancyId = TENANT;
    ObjectNode contextNode = MAPPER.createObjectNode();
    contextNode.put("subject", "Laptop won't start");
    contextNode.put("description", "Power light blinks but no display");
    instance.setContextSnapshot(contextNode);
    when(caseInstanceCache.get(CASE_ID)).thenReturn(instance);
  }

  private static AgentDescriptor minimalAgent(String agentId) {
    return AgentDescriptor.builder()
        .agentId(agentId)
        .name(agentId)
        .slot("default")
        .tenancyId(TENANT)
        .build();
  }
}
