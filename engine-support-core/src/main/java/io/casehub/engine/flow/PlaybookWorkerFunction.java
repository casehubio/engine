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
package io.casehub.engine.flow;

import io.casehub.platform.compiler.CompilationResult;
import io.casehub.worker.api.PlannedAction;
import io.serverlessworkflow.api.types.Workflow;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public record PlaybookWorkerFunction(
    Workflow workflow,
    CompilationResult compilationResult,
    Function<Map<String, Object>, PlannedAction> plannedActionFn)
    implements SWFWorkerFunction {

  public PlaybookWorkerFunction(Workflow workflow, CompilationResult compilationResult) {
    this(workflow, compilationResult, null);
  }

  public PlaybookWorkerFunction {
    java.util.Objects.requireNonNull(workflow, "workflow must not be null");
  }

  @Override
  public Class<Map<String, Object>> inputType() {
    return (Class) Map.class;
  }

  @Override
  public Class<Map<String, Object>> outputType() {
    return (Class) Map.class;
  }

  public PlaybookWorkerFunction withPlannedAction(
      Function<Map<String, Object>, PlannedAction> actionFn) {
    return new PlaybookWorkerFunction(workflow, compilationResult, actionFn);
  }
}
