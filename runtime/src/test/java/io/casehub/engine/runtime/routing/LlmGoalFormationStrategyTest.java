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
package io.casehub.engine.runtime.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.casehub.api.model.RetrievedMemory;
import io.casehub.api.model.ai.AgentException;
import io.casehub.api.spi.routing.GoalFormationContext;
import io.casehub.api.spi.routing.GoalFormationProposal;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LlmGoalFormationStrategyTest {

  @Test
  void producesProposalFromLlmResponse() {
    String json =
        "{\"goals\": [{\"name\": \"optimize-reviews\", \"description\": \"Optimize code review turnaround\", \"suggestedPriority\": \"SECONDARY\", \"formationReason\": \"Pattern of delayed reviews observed\"}], \"rationale\": \"Review bottleneck detected\"}";
    LlmGoalFormationStrategy strategy = strategyWithResponse(json);

    GoalFormationProposal proposal = strategy.propose(buildContext());
    assertThat(proposal.goals()).hasSize(1);
    assertThat(proposal.goals().get(0).name()).isEqualTo("optimize-reviews");
    assertThat(proposal.goals().get(0).formationReason())
        .isEqualTo("Pattern of delayed reviews observed");
    assertThat(proposal.rationale()).isEqualTo("Review bottleneck detected");
  }

  @Test
  void emptyGoalsArrayIsValid() {
    String json = "{\"goals\": [], \"rationale\": \"No new goals needed\"}";
    LlmGoalFormationStrategy strategy = strategyWithResponse(json);

    GoalFormationProposal proposal = strategy.propose(buildContext());
    assertThat(proposal.goals()).isEmpty();
    assertThat(proposal.rationale()).isEqualTo("No new goals needed");
  }

  @Test
  void nullPriorityDefaultsToNull() {
    String json =
        "{\"goals\": [{\"name\": \"g1\", \"description\": \"desc\", \"suggestedPriority\": null, \"formationReason\": \"reason\"}], \"rationale\": \"ok\"}";
    LlmGoalFormationStrategy strategy = strategyWithResponse(json);

    GoalFormationProposal proposal = strategy.propose(buildContext());
    assertThat(proposal.goals().get(0).suggestedPriority()).isNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  void failsWhenAgentProviderAbsent() {
    LlmGoalFormationStrategy strategy = new LlmGoalFormationStrategy(Optional.empty());

    try {
      strategy.propose(buildContext());
      fail("Expected failure");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Test
  void invalidJsonFails() {
    LlmGoalFormationStrategy strategy = strategyWithResponse("not json");

    try {
      strategy.propose(buildContext());
      fail("Expected failure");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(AgentException.class);
    }
  }

  @SuppressWarnings("unchecked")
  private LlmGoalFormationStrategy strategyWithResponse(String response) {
    AgentProvider provider =
        new AgentProvider() {
          @Override
          public Multi<AgentEvent> invoke(AgentSessionConfig config) {
            return Multi.createFrom()
                .items(
                    new AgentEvent.TextDelta(response),
                    new AgentEvent.InvocationComplete(
                        0, 0, 0, 0, 0, null, 0L, 0L, null, 0, false, null, null));
          }

          @Override
          public AgentSession openSession(AgentSessionInit init) {
            throw new UnsupportedOperationException();
          }
        };
    return new LlmGoalFormationStrategy(Optional.of(provider));
  }

  private GoalFormationContext buildContext() {
    return new GoalFormationContext(
        "agent-1",
        "tenant-1",
        List.of("Review turnaround has been slow"),
        List.of(
            new AgentGoal(
                "find-bugs",
                "Find bugs",
                GoalPriority.PRIMARY,
                Visibility.PUBLIC,
                List.of(),
                null)),
        List.of(
            new RetrievedMemory(
                "mem-1", "Past review took 3 days", "reflection", Instant.now(), Map.of())),
        9);
  }
}
