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
package io.casehub.api.model.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DecisionScopeTest {

  @Test
  void stimulus_driven_permits_only_signaling() {
    assertThat(DecisionScope.STIMULUS_DRIVEN.isPermitted(DecisionType.SIGNALING)).isTrue();
    assertThat(DecisionScope.STIMULUS_DRIVEN.isPermitted(DecisionType.TASK_SELECTION)).isFalse();
    assertThat(DecisionScope.STIMULUS_DRIVEN.isPermitted(DecisionType.COLLABORATION)).isFalse();
    assertThat(DecisionScope.STIMULUS_DRIVEN.isPermitted(DecisionType.PROVISIONING)).isFalse();
    assertThat(DecisionScope.STIMULUS_DRIVEN.isPermitted(DecisionType.CONTEXT_MUTATION)).isFalse();
  }

  @Test
  void task_adaptation_permits_collaboration_signaling_context_mutation() {
    assertThat(DecisionScope.TASK_ADAPTATION.isPermitted(DecisionType.COLLABORATION)).isTrue();
    assertThat(DecisionScope.TASK_ADAPTATION.isPermitted(DecisionType.SIGNALING)).isTrue();
    assertThat(DecisionScope.TASK_ADAPTATION.isPermitted(DecisionType.CONTEXT_MUTATION)).isTrue();
    assertThat(DecisionScope.TASK_ADAPTATION.isPermitted(DecisionType.TASK_SELECTION)).isFalse();
    assertThat(DecisionScope.TASK_ADAPTATION.isPermitted(DecisionType.PROVISIONING)).isFalse();
  }

  @Test
  void full_autonomy_permits_everything() {
    for (DecisionType type : DecisionType.values()) {
      assertThat(DecisionScope.FULL_AUTONOMY.isPermitted(type))
          .as("FULL_AUTONOMY should permit %s", type)
          .isTrue();
    }
  }
}
