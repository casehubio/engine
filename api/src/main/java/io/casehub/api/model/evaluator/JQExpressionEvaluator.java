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

import io.casehub.platform.api.expression.ExpressionEvaluator;

public record JQExpressionEvaluator(String expression) implements ExpressionEvaluator {

  public static final String TYPE = "jq";

  public static void validate(String expression) {
    if (expression != null && !expression.isBlank()) {
      try {
        net.thisptr.jackson.jq.JsonQuery.compile(
            expression, net.thisptr.jackson.jq.Versions.JQ_1_6);
      } catch (net.thisptr.jackson.jq.exception.JsonQueryException e) {
        throw new IllegalArgumentException("Invalid JQ expression: " + expression, e);
      }
    }
  }

  @Override
  public String type() {
    return TYPE;
  }
}
