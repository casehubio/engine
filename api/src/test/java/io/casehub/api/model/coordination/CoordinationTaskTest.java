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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class CoordinationTaskTest {

  @Test
  void valid_construction_all_fields() {
    var task = new CoordinationTask("t-1", "analyse input", "reviewer", "merge");
    assertThat(task.taskId()).isEqualTo("t-1");
    assertThat(task.description()).isEqualTo("analyse input");
    assertThat(task.targetAgentConstraint()).isEqualTo("reviewer");
    assertThat(task.aggregationHint()).isEqualTo("merge");
  }

  @Test
  void valid_construction_optional_fields_null() {
    var task = new CoordinationTask("t-2", "summarise", null, null);
    assertThat(task.targetAgentConstraint()).isNull();
    assertThat(task.aggregationHint()).isNull();
  }

  @Test
  void null_taskId_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationTask(null, "desc", null, null))
        .withMessageContaining("taskId");
  }

  @Test
  void blank_taskId_rejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CoordinationTask("  ", "desc", null, null))
        .withMessageContaining("taskId");
  }

  @Test
  void null_description_rejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CoordinationTask("t-1", null, null, null))
        .withMessageContaining("description");
  }
}
