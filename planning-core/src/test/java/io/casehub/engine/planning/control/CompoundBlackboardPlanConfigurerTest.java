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
package io.casehub.engine.planning.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.PlanExecutionContext;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.CompoundDeclaration;
import io.casehub.api.model.Participation;
import io.casehub.engine.planning.plan.CompletionSemantics;
import io.casehub.engine.planning.plan.DefaultCasePlanModel;
import io.casehub.engine.planning.plan.PlanItemDefinition;
import io.casehub.platform.api.identity.TenancyConstants;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompoundBlackboardPlanConfigurerTest {

  private PlanExecutionContext ctx(CaseDefinition def) {
    return new PlanExecutionContext(
        UUID.randomUUID(),
        def,
        mock(CaseContext.class),
        CaseStatus.RUNNING,
        TenancyConstants.DEFAULT_TENANT_ID,
        List.of(),
        null,
        null);
  }

  @Test
  void registersCompoundWithMofNSemantics() {
    var compound =
        new CompoundDeclaration(
            "extraction",
            "3",
            null,
            Map.of(
                "a", Participation.PARTICIPANT,
                "b", Participation.PARTICIPANT,
                "c", Participation.PARTICIPANT),
            null,
            null,
            false,
            null);

    var def =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0.0")
            .compound(compound)
            .build();

    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    new CompoundBlackboardPlanConfigurer().configure(plan, ctx(def));

    var compounds = plan.getAllCompounds();
    assertThat(compounds).hasSize(1);
    PlanItemDefinition.Compound registered = compounds.get(0);
    assertThat(registered.name()).isEqualTo("extraction");
    assertThat(registered.completion()).isInstanceOf(CompletionSemantics.MOfN.class);
    assertThat(((CompletionSemantics.MOfN) registered.completion()).m()).isEqualTo(3);
  }

  @Test
  void defaultsToAllSemantics() {
    var compound =
        new CompoundDeclaration(
            "phase1", null, null, Map.of("a", Participation.PARTICIPANT), null, null, false, null);

    var def =
        CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0.0")
            .compound(compound)
            .build();

    var plan = new DefaultCasePlanModel(UUID.randomUUID());
    new CompoundBlackboardPlanConfigurer().configure(plan, ctx(def));

    var compounds = plan.getAllCompounds();
    assertThat(compounds).hasSize(1);
    assertThat(compounds.get(0).completion()).isInstanceOf(CompletionSemantics.All.class);
  }

  @Test
  void parsesFirstWins() {
    assertThat(CompoundBlackboardPlanConfigurer.parseCompletionSemantics("firstWins"))
        .isInstanceOf(CompletionSemantics.FirstWins.class);
  }

  @Test
  void rejectsUnrecognizedSemantics() {
    assertThatThrownBy(() -> CompoundBlackboardPlanConfigurer.parseCompletionSemantics("invalid"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
