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
package io.casehub.api.model.stigmergy;

import java.util.Set;

public enum ImprovementStage {
  INTROSPECT,
  RESEARCH_SCOPE,
  SEARCH,
  ANALYZE,
  HYPOTHESIS_APPROVAL,
  IMPLEMENTATION_PLAN,
  IMPLEMENT,
  SUBMIT_PR,
  PR_REVIEW,
  INTEGRATE,
  OUTCOME_RECORDING;

  private static final Set<ImprovementStage> GATE_CHECKPOINTS =
      Set.of(RESEARCH_SCOPE, HYPOTHESIS_APPROVAL, IMPLEMENTATION_PLAN, PR_REVIEW);

  public boolean isGateCheckpoint() {
    return GATE_CHECKPOINTS.contains(this);
  }
}
