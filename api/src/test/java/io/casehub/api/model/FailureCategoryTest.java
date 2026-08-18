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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FailureCategoryTest {

  @Test
  void transient_category_name() {
    var cat = new FailureCategory.Transient("timeout");
    assertThat(cat.categoryName()).isEqualTo("transient");
    assertThat(cat.reason()).isEqualTo("timeout");
  }

  @Test
  void knowledge_category_name() {
    var cat = new FailureCategory.Knowledge("not found", "entityId");
    assertThat(cat.categoryName()).isEqualTo("knowledge");
    assertThat(cat.missingContext()).isEqualTo("entityId");
  }

  @Test
  void knowledge_null_missing_context() {
    var cat = new FailureCategory.Knowledge("declined", null);
    assertThat(cat.missingContext()).isNull();
  }

  @Test
  void infeasible_category_name() {
    var cat = new FailureCategory.Infeasible("all attempts exhausted");
    assertThat(cat.categoryName()).isEqualTo("infeasible");
  }

  @Test
  void pattern_matching() {
    FailureCategory cat = new FailureCategory.Transient("test");
    String result =
        switch (cat) {
          case FailureCategory.Transient t -> "transient: " + t.reason();
          case FailureCategory.Knowledge k -> "knowledge: " + k.reason();
          case FailureCategory.Infeasible i -> "infeasible: " + i.reason();
        };
    assertThat(result).isEqualTo("transient: test");
  }
}
