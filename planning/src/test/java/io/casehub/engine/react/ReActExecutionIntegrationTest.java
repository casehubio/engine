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
class ReActExecutionIntegrationTest {

  @Inject CaseInstanceCache cache;
  @Inject ReActCaseHub caseHub;

  @BeforeEach
  void clear() {
    cache.clear();
    ReActCaseHub.callCount.set(0);
  }

  @Test
  void reactWorker_completesCase() throws Exception {
    UUID caseId = caseHub.startCase(Map.of("query", "find anomalies"));

    await()
        .atMost(30, TimeUnit.SECONDS)
        .until(
            () -> {
              var instance = cache.get(caseId);
              return instance != null && instance.getState() == CaseStatus.COMPLETED;
            });

    var instance = cache.get(caseId);
    assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
    assertThat(instance.getCaseContext().get("research")).isNotNull();
  }

  @ApplicationScoped
  public static class ReActCaseHub extends CaseHub {

    static final AtomicInteger callCount = new AtomicInteger(0);

    private final Capability cap =
        Capability.builder()
            .name("research")
            .inputSchema("{ query: .query }")
            .outputSchema(".")
            .build();

    private final Goal goal =
        Goal.builder().name("done").condition(".research != null").kind(GoalKind.SUCCESS).build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-react")
          .name("react-integration")
          .version("1.0.0")
          .capabilities(cap)
          .workers(
              Worker.builder()
                  .name("react-researcher")
                  .capabilityName("research")
                  .function(
                      new ReActWorkerFunction(
                          new StubChatModel(),
                          "You are a researcher. Use tools to find information.",
                          List.of(
                              new ToolSource.LocalTool(
                                  "search",
                                  "Search for information",
                                  args -> Map.of("results", "found 3 anomalies"),
                                  Map.of(
                                      "type",
                                      "object",
                                      "properties",
                                      Map.of("query", Map.of("type", "string")))))))
                  .build())
          .bindings(
              Binding.builder()
                  .name("do-research")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".query != null and .research == null"))
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
                          .name("search")
                          .arguments("{\"query\": \"anomalies\"}")
                          .build()))
              .build();
        }
        return ChatResponse.builder()
            .aiMessage(AiMessage.from("{\"research\": \"Found 3 anomalies in the dataset\"}"))
            .build();
      }
    }
  }
}
