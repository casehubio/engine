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

import io.casehub.api.engine.PlanExecutionContext;
import io.casehub.api.model.CompoundDeclaration;
import io.casehub.engine.planning.plan.CasePlanModel;
import io.casehub.engine.planning.plan.CompletionSemantics;
import io.casehub.engine.planning.plan.DispatchMode;
import io.casehub.engine.planning.plan.PlanItemDefinition;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CompoundBlackboardPlanConfigurer implements BlackboardPlanConfigurer {

  @Override
  public void configure(CasePlanModel plan, PlanExecutionContext context) {
    if (context.definition().getCompounds() == null) return;
    for (CompoundDeclaration cd : context.definition().getCompounds()) {
      CompletionSemantics completion = parseCompletionSemantics(cd.completionSemantics());
      DispatchMode dispatch = parseDispatchMode(cd.dispatchMode());
      var builder =
          PlanItemDefinition.Compound.builder(cd.name())
              .completion(completion)
              .dispatchMode(dispatch)
              .repeatable(cd.repeatable());
      if (cd.entryCondition() != null) builder.entryCondition(cd.entryCondition());
      if (cd.exitCondition() != null) builder.exitCondition(cd.exitCondition());
      if (cd.planningStrategy() != null) builder.planningStrategy(cd.planningStrategy());
      if (cd.scopedBindings() != null) {
        cd.scopedBindings().forEach(builder::binding);
      }
      plan.registerDefinition(builder.build());
    }
  }

  static CompletionSemantics parseCompletionSemantics(String value) {
    if (value == null || value.equalsIgnoreCase("all")) return CompletionSemantics.all();
    if (value.equalsIgnoreCase("firstWins")) return CompletionSemantics.firstWins();
    try {
      int m = Integer.parseInt(value);
      return CompletionSemantics.mOfN(m);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Unrecognized completionSemantics: '" + value + "'");
    }
  }

  static DispatchMode parseDispatchMode(String value) {
    if (value == null) return DispatchMode.CHOREOGRAPHED;
    return DispatchMode.valueOf(value.toUpperCase());
  }
}
