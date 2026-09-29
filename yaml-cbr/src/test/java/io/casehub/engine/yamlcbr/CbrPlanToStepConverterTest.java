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
package io.casehub.engine.yamlcbr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.spi.routing.ExperiencePlanStep;
import io.casehub.api.spi.routing.RoutingOutcome;
import io.casehub.yaml.step.CatalogEntry;
import io.casehub.yaml.step.StepCatalog;
import io.casehub.yaml.step.catalog.ResolvedStep;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CbrPlanToStepConverterTest {

  private StubCatalog catalog;
  private CbrPlanToStepConverter converter;

  @BeforeEach
  void setUp() {
    catalog = new StubCatalog();
    converter = new CbrPlanToStepConverter(catalog);
  }

  @Test
  void convertsStepsThroughCatalog() {
    catalog.register("triage");
    var step =
        new ExperiencePlanStep(
            "triage-binding", "triage", "agent-1", RoutingOutcome.SUCCESS, 0, Map.of("key", "val"));
    var result = converter.convert(List.of(step));
    assertEquals(1, result.size());
    assertInstanceOf(ResolvedStep.PluginStep.class, result.get(0));
    assertEquals("triage", result.get(0).name());
  }

  @Test
  void filtersRemovedSteps() {
    catalog.register("triage");
    var step =
        new ExperiencePlanStep(
            "triage-binding",
            "triage",
            "agent-1",
            RoutingOutcome.SUCCESS,
            0,
            Map.of(),
            "REMOVED",
            "outdated");
    var result = converter.convert(List.of(step));
    assertTrue(result.isEmpty());
  }

  @Test
  void filtersSuppressedSteps() {
    catalog.register("triage");
    var step =
        new ExperiencePlanStep(
            "triage-binding",
            "triage",
            "agent-1",
            RoutingOutcome.SUCCESS,
            0,
            Map.of(),
            "SUPPRESSED",
            "low confidence");
    var result = converter.convert(List.of(step));
    assertTrue(result.isEmpty());
  }

  @Test
  void allowsUnknownAdaptationActions() {
    catalog.register("triage");
    var step =
        new ExperiencePlanStep(
            "triage-binding",
            "triage",
            "agent-1",
            RoutingOutcome.SUCCESS,
            0,
            Map.of(),
            "FUTURE_ACTION",
            null);
    var result = converter.convert(List.of(step));
    assertEquals(1, result.size());
  }

  @Test
  void skipsNullCapabilityName() {
    var step =
        new ExperiencePlanStep("binding", null, "agent-1", RoutingOutcome.SUCCESS, 0, Map.of());
    var result = converter.convert(List.of(step));
    assertTrue(result.isEmpty());
  }

  @Test
  void skipsUnresolvableSteps() {
    var step =
        new ExperiencePlanStep(
            "binding", "nonexistent", "agent-1", RoutingOutcome.SUCCESS, 0, Map.of());
    var result = converter.convert(List.of(step));
    assertTrue(result.isEmpty());
  }

  @Test
  void sortsStepsByPriority() {
    catalog.register("step-a");
    catalog.register("step-b");
    var high =
        new ExperiencePlanStep("b", "step-b", "agent-1", RoutingOutcome.SUCCESS, 10, Map.of());
    var low = new ExperiencePlanStep("a", "step-a", "agent-1", RoutingOutcome.SUCCESS, 1, Map.of());
    var result = converter.convert(List.of(high, low));
    assertEquals(2, result.size());
    assertEquals("step-a", result.get(0).name());
    assertEquals("step-b", result.get(1).name());
  }

  @Test
  void emptyInputProducesEmptyOutput() {
    var result = converter.convert(List.of());
    assertTrue(result.isEmpty());
  }

  // --- Test double ---

  static class StubCatalog implements StepCatalog {
    private final Set<String> registered = new HashSet<>();

    void register(String actionName) {
      registered.add(actionName);
    }

    @Override
    public Optional<CatalogEntry> resolve(String actionName) {
      if (registered.contains(actionName)) {
        return Optional.of(new CatalogEntry(actionName, null, null));
      }
      return Optional.empty();
    }

    @Override
    public Set<String> availableActions() {
      return registered;
    }
  }
}
