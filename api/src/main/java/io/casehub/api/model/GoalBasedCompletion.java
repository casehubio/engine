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

// TODO this must be replaced by a more generic implementation that can support multiple goals of
// different kinds, not just success and failure
public class GoalBasedCompletion implements CaseCompletion {

  private final GoalExpression success;
  private final GoalExpression failure;

  public GoalBasedCompletion(GoalExpression success, GoalExpression failure) {
    this.success = success;
    this.failure = failure;
  }

  public GoalExpression getSuccess() {
    return success;
  }

  public GoalExpression getFailure() {
    return failure;
  }
}
