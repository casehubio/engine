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
package io.casehub.engine.planning.adaptation;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.FailureCategory;
import io.casehub.api.model.TaskStatus;
import io.casehub.engine.plan.adaptation.AdaptationContext;
import io.casehub.engine.plan.adaptation.AdaptationDecision;
import io.casehub.engine.plan.adaptation.MetaReasoningContext;
import io.casehub.engine.plan.adaptation.RefineScope;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CostCeilingMetaReasonerTest {

  private final CostCeilingMetaReasoner reasoner = new CostCeilingMetaReasoner();

  private MetaReasoningContext context(int adaptationCount, FailureCategory category) {
    return context(adaptationCount, category, null);
  }

  private MetaReasoningContext context(
      int adaptationCount, FailureCategory category, Integer maxAdaptations) {
    var def = new CaseDefinition("ns", "test", "1.0");
    if (maxAdaptations != null) def.setMaxAdaptations(maxAdaptations);
    var ac =
        new AdaptationContext(
            UUID.randomUUID(),
            "tenant-1",
            "comp-1",
            "goal-1",
            List.of(),
            List.of(),
            List.of(),
            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
            def,
            TaskStatus.COMPLETED,
            "binding-1",
            adaptationCount);
    return new MetaReasoningContext(ac, adaptationCount, 3, 5, 8, category);
  }

  @Test
  void concede_when_adaptation_ceiling_reached() {
    var decision = reasoner.evaluate(context(5, null));
    assertThat(decision).isInstanceOf(AdaptationDecision.Concede.class);
    assertThat(decision.reason()).contains("ceiling");
  }

  @Test
  void concede_with_custom_max_adaptations() {
    var decision = reasoner.evaluate(context(3, null, 3));
    assertThat(decision).isInstanceOf(AdaptationDecision.Concede.class);
  }

  @Test
  void concede_on_infeasible_failure() {
    var decision =
        reasoner.evaluate(context(0, new FailureCategory.Infeasible("goal contradiction")));
    assertThat(decision).isInstanceOf(AdaptationDecision.Concede.class);
    assertThat(decision.reason()).contains("Infeasible");
  }

  @Test
  void persist_on_transient_failure() {
    var decision = reasoner.evaluate(context(0, new FailureCategory.Transient("timeout")));
    assertThat(decision).isInstanceOf(AdaptationDecision.Persist.class);
    assertThat(decision.reason()).contains("Transient");
  }

  @Test
  void refine_local_on_first_knowledge_failure() {
    var decision =
        reasoner.evaluate(context(0, new FailureCategory.Knowledge("missing data", "entityId")));
    assertThat(decision).isInstanceOf(AdaptationDecision.Refine.class);
    assertThat(((AdaptationDecision.Refine) decision).scope()).isEqualTo(RefineScope.LOCAL);
  }

  @Test
  void refine_compound_on_repeated_knowledge_failure() {
    var decision =
        reasoner.evaluate(context(2, new FailureCategory.Knowledge("missing data", "entityId")));
    assertThat(decision).isInstanceOf(AdaptationDecision.Refine.class);
    assertThat(((AdaptationDecision.Refine) decision).scope()).isEqualTo(RefineScope.COMPOUND);
  }

  @Test
  void refine_compound_on_null_failure_category() {
    var decision = reasoner.evaluate(context(0, null));
    assertThat(decision).isInstanceOf(AdaptationDecision.Refine.class);
    assertThat(((AdaptationDecision.Refine) decision).scope()).isEqualTo(RefineScope.COMPOUND);
  }

  @Test
  void default_max_adaptations_is_5() {
    var decision4 = reasoner.evaluate(context(4, null));
    assertThat(decision4).isInstanceOf(AdaptationDecision.Refine.class);
    var decision5 = reasoner.evaluate(context(5, null));
    assertThat(decision5).isInstanceOf(AdaptationDecision.Concede.class);
  }

  @Test
  void ceiling_takes_precedence_over_failure_category() {
    var decision = reasoner.evaluate(context(5, new FailureCategory.Knowledge("data", null)));
    assertThat(decision).isInstanceOf(AdaptationDecision.Concede.class);
    assertThat(decision.reason()).contains("ceiling");
  }

  @Test
  void infeasible_takes_precedence_over_transient() {
    var decision = reasoner.evaluate(context(0, new FailureCategory.Infeasible("cannot achieve")));
    assertThat(decision).isInstanceOf(AdaptationDecision.Concede.class);
  }

  @Test
  void id_is_cost_ceiling() {
    assertThat(reasoner.id()).isEqualTo("cost-ceiling");
  }
}
