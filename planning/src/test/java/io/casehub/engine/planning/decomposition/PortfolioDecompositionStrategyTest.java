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
package io.casehub.engine.planning.decomposition;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ai.AgentException;
import io.casehub.engine.plan.DagPlan;
import io.casehub.engine.plan.DecompositionContext;
import io.casehub.engine.plan.DecompositionStrategy;
import io.casehub.engine.plan.PortfolioConfig;
import io.casehub.engine.plan.TaskNode;
import io.casehub.platform.api.routing.NamedStrategy;
import io.casehub.platform.api.routing.StrategyResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortfolioDecompositionStrategyTest {

  private TestStrategyResolver resolver;
  private PortfolioDecompositionStrategy portfolio;

  @BeforeEach
  void setUp() {
    resolver = new TestStrategyResolver();
    portfolio = new PortfolioDecompositionStrategy(resolver);
  }

  @Test
  void idIsPortfolio() {
    assertEquals("portfolio", portfolio.id());
  }

  @Test
  void firstStrategySucceedsReturnedImmediately() {
    var plan = singletonPlan("step-1");
    resolver.register("goap", succeeding(plan));
    var secondCalled = new AtomicBoolean(false);
    resolver.register(
        "llm",
        new DecompositionStrategy<>() {
          @Override
          public DagPlan<TaskNode.LeafTask<JsonNode>> decompose(
              TaskNode<JsonNode> task, DecompositionContext<JsonNode> context) {
            secondCalled.set(true);
            return plan;
          }

          @Override
          public String id() {
            return "llm";
          }
        });

    var result =
        portfolio.decompose(
            dummyTask(), contextWithConfig(new PortfolioConfig(List.of("goap", "llm"), null)));

    assertNotNull(result);
    assertFalse(secondCalled.get());
  }

  @Test
  void firstStrategyFailsCascadesToSecond() {
    resolver.register("goap", throwing("GOAP failed"));
    var plan = singletonPlan("step-1");
    resolver.register("llm", succeeding(plan));

    var result =
        portfolio.decompose(
            dummyTask(), contextWithConfig(new PortfolioConfig(List.of("goap", "llm"), null)));

    assertNotNull(result);
    assertEquals(1, result.nodes().size());
  }

  @Test
  void timeoutCascadesToNext() {
    resolver.register(
        "goap",
        new DecompositionStrategy<>() {
          @Override
          public DagPlan<TaskNode.LeafTask<JsonNode>> decompose(
              TaskNode<JsonNode> task, DecompositionContext<JsonNode> context) {
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new AgentException("interrupted");
            }
            return singletonPlan("x");
          }

          @Override
          public String id() {
            return "goap";
          }
        });
    var plan = singletonPlan("step-1");
    resolver.register("llm", succeeding(plan));

    var result =
        portfolio.decompose(
            dummyTask(),
            contextWithConfig(
                new PortfolioConfig(List.of("goap", "llm"), Map.of("goap", 100L, "llm", 30000L))));

    assertNotNull(result);
  }

  @Test
  void unknownDelegateSkipped() {
    var plan = singletonPlan("step-1");
    resolver.register("llm", succeeding(plan));

    var result =
        portfolio.decompose(
            dummyTask(),
            contextWithConfig(new PortfolioConfig(List.of("nonexistent", "llm"), null)));

    assertNotNull(result);
  }

  @Test
  void allDelegatesFailThrowsAgentException() {
    resolver.register("goap", throwing("GOAP failed"));
    resolver.register("llm", throwing("LLM failed"));

    var ex =
        assertThrows(
            AgentException.class,
            () ->
                portfolio.decompose(
                    dummyTask(),
                    contextWithConfig(new PortfolioConfig(List.of("goap", "llm"), null))));

    assertTrue(ex.getMessage().contains("Portfolio"));
  }

  @Test
  void selfReferenceFilteredOut() {
    resolver.register("portfolio", throwing("should not be called"));
    var plan = singletonPlan("step-1");
    resolver.register("llm", succeeding(plan));

    var result =
        portfolio.decompose(
            dummyTask(), contextWithConfig(new PortfolioConfig(List.of("portfolio", "llm"), null)));

    assertNotNull(result);
  }

  @Test
  void defaultConfigUsedWhenPortfolioConfigNull() {
    var plan = singletonPlan("step-1");
    resolver.register("goap", succeeding(plan));

    var result = portfolio.decompose(dummyTask(), contextWithConfig(null));

    assertNotNull(result);
  }

  // --- helpers ---

  private GoalDecompositionContext contextWithConfig(PortfolioConfig config) {
    var def = CaseDefinition.builder().namespace("test").name("test").version("1.0").build();
    if (config != null) {
      def.setPortfolioConfig(config);
    }
    return new GoalDecompositionContext(
        JsonNodeFactory.instance.objectNode(), 0, List.of(), null, def);
  }

  private TaskNode<JsonNode> dummyTask() {
    return new GoalStep(UUID.randomUUID(), "test-step", "test-cap", Instant.now());
  }

  private DagPlan<TaskNode.LeafTask<JsonNode>> singletonPlan(String id) {
    var step = new GoalStep(UUID.randomUUID(), id, id, Instant.now());
    return DagPlan.singleton(id, step);
  }

  private DecompositionStrategy<JsonNode> succeeding(DagPlan<TaskNode.LeafTask<JsonNode>> plan) {
    return new DecompositionStrategy<>() {
      @Override
      public DagPlan<TaskNode.LeafTask<JsonNode>> decompose(
          TaskNode<JsonNode> task, DecompositionContext<JsonNode> context) {
        return plan;
      }

      @Override
      public String id() {
        return "test";
      }
    };
  }

  private DecompositionStrategy<JsonNode> throwing(String msg) {
    return new DecompositionStrategy<>() {
      @Override
      public DagPlan<TaskNode.LeafTask<JsonNode>> decompose(
          TaskNode<JsonNode> task, DecompositionContext<JsonNode> context) {
        throw new AgentException(msg);
      }

      @Override
      public String id() {
        return "test";
      }
    };
  }

  static class TestStrategyResolver implements StrategyResolver {

    private final Map<String, NamedStrategy> strategies = new HashMap<>();

    void register(String id, DecompositionStrategy<JsonNode> strategy) {
      strategies.put(id, strategy);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NamedStrategy> T resolve(Class<T> type, String id) {
      T s = (T) strategies.get(id);
      if (s == null) {
        throw new IllegalArgumentException("No strategy: " + id);
      }
      return s;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NamedStrategy> Optional<T> find(Class<T> type, String id) {
      return Optional.ofNullable((T) strategies.get(id));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NamedStrategy> T defaultStrategy(Class<T> type) {
      throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NamedStrategy> List<T> available(Class<T> type) {
      return (List<T>) new ArrayList<>(strategies.values());
    }
  }
}
