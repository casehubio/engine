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
package io.casehub.engine.react;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReActAuditTrailTest {

  @Inject CaseInstanceCache cache;
  @Inject EventLogRepository eventLogRepository;
  @Inject AuditTrailCaseHub caseHub;

  @BeforeEach
  void clear() {
    cache.clear();
    AuditTrailCaseHub.callCount.set(0);
  }

  @Test
  void reactCycleEvents_areWrittenToEventLog() throws Exception {
    UUID caseId = caseHub.startCase(Map.of("query", "audit test"));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              var instance = cache.get(caseId);
              return instance != null && instance.getState() == CaseStatus.COMPLETED;
            });

    String tenancyId = cache.get(caseId).tenancyId;
    List<EventLog> cycleEvents =
        eventLogRepository.findByCaseAndTypes(
            caseId, List.of(CaseHubEventType.REACT_CYCLE), tenancyId);

    assertThat(cycleEvents).isNotEmpty();
    for (EventLog log : cycleEvents) {
      assertThat(log.getMetadata()).isNotNull();
      assertThat(log.getMetadata().has("cycleIndex")).isTrue();
    }

    EventLog firstCycle =
        cycleEvents.stream()
            .filter(e -> e.getMetadata().get("cycleIndex").asInt() == 0)
            .findFirst()
            .orElse(null);
    assertThat(firstCycle).isNotNull();
    assertThat(firstCycle.getMetadata().has("toolCalls")).isTrue();
    assertThat(firstCycle.getMetadata().get("toolCalls").isArray()).isTrue();
    assertThat(firstCycle.getMetadata().get("toolCalls").size()).isGreaterThan(0);
  }

  @Test
  void workerCompletionEvent_carriesReactMetadata() throws Exception {
    UUID caseId = caseHub.startCase(Map.of("query", "metadata test"));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              var instance = cache.get(caseId);
              return instance != null && instance.getState() == CaseStatus.COMPLETED;
            });

    String tenancyId = cache.get(caseId).tenancyId;
    List<EventLog> completionEvents =
        eventLogRepository.findByCaseAndTypes(
            caseId, List.of(CaseHubEventType.WORKER_EXECUTION_COMPLETED), tenancyId);

    assertThat(completionEvents).isNotEmpty();
    EventLog completion = completionEvents.get(completionEvents.size() - 1);
    assertThat(completion.getMetadata()).isNotNull();
    assertThat(completion.getMetadata().has("reactCycleCount")).isTrue();
    assertThat(completion.getMetadata().get("reactCycleCount").asInt()).isGreaterThan(0);
  }

  @ApplicationScoped
  public static class AuditTrailCaseHub extends CaseHub {

    static final AtomicInteger callCount = new AtomicInteger(0);

    private final Capability cap =
        Capability.builder()
            .name("audit-research")
            .inputSchema("{ query: .query }")
            .outputSchema(".")
            .build();

    private final Goal goal =
        Goal.builder()
            .name("done")
            .condition(".\"audit-research\" != null")
            .kind(GoalKind.SUCCESS)
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-react-audit")
          .name("react-audit")
          .version("1.0.0")
          .capabilities(cap)
          .workers(
              Worker.builder()
                  .name("audit-researcher")
                  .capabilityName("audit-research")
                  .function(
                      new ReActWorkerFunction(
                          new StubChatModel(),
                          "You are a researcher.",
                          List.of(
                              new ToolSource.LocalTool(
                                  "lookup",
                                  "Look up data",
                                  args -> Map.of("data", "found"),
                                  Map.of(
                                      "type",
                                      "object",
                                      "properties",
                                      Map.of("term", Map.of("type", "string")))))))
                  .build())
          .bindings(
              Binding.builder()
                  .name("do-audit")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".query != null and .\"audit-research\" == null"))
                  .build())
          .goals(goal)
          .completion(GoalExpression.allOf(goal))
          .build();
    }

    private static class StubChatModel implements ChatModel {
      @Override
      public ChatResponse chat(ChatRequest chatRequest) {
        int call = callCount.incrementAndGet();
        if (call == 1) {
          return ChatResponse.builder()
              .aiMessage(
                  AiMessage.from(
                      ToolExecutionRequest.builder()
                          .id("call-1")
                          .name("lookup")
                          .arguments("{\"term\": \"anomalies\"}")
                          .build()))
              .build();
        }
        return ChatResponse.builder()
            .aiMessage(AiMessage.from("{\"audit-research\": \"Audit complete\"}"))
            .build();
      }
    }
  }
}
