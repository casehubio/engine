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
package io.casehub.api.model.evaluator;

import io.casehub.api.context.StateContext;
import java.util.function.Predicate;

/**
 * An {@link ExpressionEvaluator} backed by a Java lambda. Provides type-safe, IDE-friendly
 * conditions for Java DSL users as an alternative to {@link JQExpressionEvaluator} JQ strings.
 *
 * <p>Not serialisable — use {@link JQExpressionEvaluator} for YAML-defined cases.
 */
public final class LambdaExpressionEvaluator implements ExpressionEvaluator {

  private final Predicate<StateContext> predicate;

  public LambdaExpressionEvaluator(final Predicate<StateContext> predicate) {
    this.predicate = predicate;
  }

  public boolean test(final StateContext context) {
    return predicate.test(context);
  }
}
