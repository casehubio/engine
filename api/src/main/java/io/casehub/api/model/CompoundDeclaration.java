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

import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.util.Map;

/**
 * YAML-declared compound task definition. Captures the structural declaration from the YAML DSL;
 * the engine converts these to runtime PlanItemDefinition.Compound instances.
 *
 * @param name compound name — unique within the definition
 * @param completionSemantics how children complete: "all" (default), "firstWins", or integer for
 *     M-of-N
 * @param dispatchMode "ORCHESTRATED" (sequential) or "CHOREOGRAPHED" (parallel, default)
 * @param scopedBindings bindings scoped to this compound, with participation level
 * @param entryCondition optional expression that must be true before this compound activates
 * @param exitCondition optional expression that forces completion when true
 * @param repeatable whether this compound can be re-entered after completion
 * @param planningStrategy optional planning strategy override for this compound's children
 */
public record CompoundDeclaration(
    String name,
    String completionSemantics,
    String dispatchMode,
    Map<String, Participation> scopedBindings,
    ExpressionEvaluator entryCondition,
    ExpressionEvaluator exitCondition,
    boolean repeatable,
    String planningStrategy) {

  public CompoundDeclaration {
    java.util.Objects.requireNonNull(name, "compound name required");
    if (completionSemantics == null) completionSemantics = "all";
    if (dispatchMode == null) dispatchMode = "CHOREOGRAPHED";
    scopedBindings = scopedBindings != null ? Map.copyOf(scopedBindings) : Map.of();
  }
}
