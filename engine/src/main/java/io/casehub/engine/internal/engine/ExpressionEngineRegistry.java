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
package io.casehub.engine.internal.engine;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.context.StateContext;
import io.casehub.api.engine.ExpressionEngine;
import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.engine.internal.context.StateContextImpl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Dispatches expression evaluation to the appropriate {@link ExpressionEngine} by evaluator type.
 *
 * <p>All CDI beans implementing {@link ExpressionEngine} are discovered automatically. Add a new
 * engine bean to support additional expression languages without modifying this class or the
 * runtime.
 */
@ApplicationScoped
public class ExpressionEngineRegistry {

  private static final Logger LOG = Logger.getLogger(ExpressionEngineRegistry.class);

  @Inject Instance<ExpressionEngine> engines;

  /**
   * Evaluates the expression against the given context.
   *
   * @param evaluator the expression to evaluate; returns {@code true} if {@code null}
   * @param context the current case state
   * @return {@code true} if the expression matches or is absent
   * @throws IllegalArgumentException if no engine is registered for the evaluator type
   */
  public boolean evaluate(final ExpressionEvaluator evaluator, final StateContext context) {
    if (evaluator == null) {
      return true;
    }
    final String type = evaluator.type();
    for (ExpressionEngine engine : engines) {
      if (engine.type().equals(type)) {
        return engine.evaluate(evaluator, context);
      }
    }
    throw new IllegalArgumentException("No ExpressionEngine registered for type '" + type + "'");
  }

  // TODO do not use JsonNode
  public boolean evaluate(final ExpressionEvaluator evaluator, final JsonNode asNode) {
    return evaluate(evaluator, new StateContextImpl(asNode));
  }

  /**
   * Validates the expression syntax without evaluating it against any context. Blocks case
   * definition registration if the expression is invalid.
   *
   * @param evaluator the expression to validate; no-op if {@code null}
   * @throws IllegalArgumentException if the expression is syntactically invalid or no engine is
   *     registered for the evaluator type
   */
  public void validate(final ExpressionEvaluator evaluator) {
    if (evaluator == null) {
      return;
    }
    final String type = evaluator.type();
    for (ExpressionEngine engine : engines) {
      if (engine.type().equals(type)) {
        engine.validate(evaluator);
        return;
      }
    }
    throw new IllegalArgumentException("No ExpressionEngine registered for type '" + type + "'");
  }
}
