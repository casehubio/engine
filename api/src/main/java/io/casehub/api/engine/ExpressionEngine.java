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
package io.casehub.api.engine;

import io.casehub.api.context.StateContext;
import io.casehub.api.model.evaluator.ExpressionEvaluator;

/**
 * SPI for pluggable expression evaluation engines.
 *
 * <p>Each engine declares the {@link ExpressionEvaluator#type()} it handles and evaluates
 * expressions of that type against a {@link StateContext}. Register additional engines as CDI beans
 * to support new expression languages (e.g. Drools, SpEL) without modifying the runtime.
 *
 * @see io.casehub.api.model.evaluator.JQExpressionEvaluator
 * @see io.casehub.api.model.evaluator.LambdaExpressionEvaluator
 */
public interface ExpressionEngine {

  /**
   * Returns the evaluator type this engine handles, matching {@link ExpressionEvaluator#type()}.
   */
  String type();

  /**
   * Evaluates the expression against the given context.
   *
   * @param evaluator the expression to evaluate — guaranteed to match {@link #type()}
   * @param context the current case state
   * @return {@code true} if the expression matches, {@code false} otherwise
   */
  boolean evaluate(ExpressionEvaluator evaluator, StateContext context);
}
