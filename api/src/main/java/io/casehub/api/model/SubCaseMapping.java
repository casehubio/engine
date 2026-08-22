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
package io.casehub.api.model;

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.Objects;
import java.util.function.Function;

public sealed interface SubCaseMapping permits SubCaseMapping.Expression, SubCaseMapping.Lambda {

  static SubCaseMapping of(String expression) {
    Objects.requireNonNull(expression, "expression");
    if (expression.isBlank()) {
      throw new IllegalArgumentException("expression must not be blank");
    }
    return new Expression(new JQExpressionEvaluator(expression));
  }

  static SubCaseMapping of(Function<CaseContext, Object> fn) {
    return new Lambda(fn);
  }

  record Expression(ExpressionEvaluator evaluator) implements SubCaseMapping {
    public Expression {
      Objects.requireNonNull(evaluator, "evaluator");
    }
  }

  record Lambda(Function<CaseContext, Object> fn) implements SubCaseMapping {
    public Lambda {
      Objects.requireNonNull(fn, "fn");
    }
  }
}
