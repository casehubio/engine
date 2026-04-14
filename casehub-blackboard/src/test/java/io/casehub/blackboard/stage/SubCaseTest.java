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
package io.casehub.blackboard.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.plan.PlanElement;
import io.casehub.blackboard.strategy.DefaultSubCaseCompletionStrategy;
import org.junit.jupiter.api.Test;

class SubCaseTest {

  @Test
  void subCaseImplementsPlanElement() {
    SubCase sc = SubCase.builder().namespace("ns").name("n").version("1.0").build();
    assertThat(sc).isInstanceOf(PlanElement.class);
  }

  @Test
  void namespaceRequired() {
    assertThatThrownBy(() -> SubCase.builder().name("n").version("1.0").build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void nameRequired() {
    assertThatThrownBy(() -> SubCase.builder().namespace("ns").version("1.0").build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void versionRequired() {
    assertThatThrownBy(() -> SubCase.builder().namespace("ns").name("n").build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void defaultCompletionStrategyIsUsedWhenNotSpecified() {
    SubCase sc = SubCase.builder().namespace("ns").name("n").version("1.0").build();
    assertThat(sc.getCompletionStrategy()).isInstanceOf(DefaultSubCaseCompletionStrategy.class);
  }

  @Test
  void customCompletionStrategyIsRetained() {
    io.casehub.blackboard.strategy.SubCaseCompletionStrategy strategy =
        (status, ctx) -> io.casehub.blackboard.plan.PlanItemStatus.TERMINATED;
    SubCase sc =
        SubCase.builder()
            .namespace("ns")
            .name("n")
            .version("1.0")
            .completionStrategy(strategy)
            .build();
    assertThat(sc.getCompletionStrategy()).isSameAs(strategy);
  }

  @Test
  void fieldsAreRetained() {
    SubCase sc = SubCase.builder().namespace("ns").name("my-case").version("2.0").build();
    assertThat(sc.getNamespace()).isEqualTo("ns");
    assertThat(sc.getName()).isEqualTo("my-case");
    assertThat(sc.getVersion()).isEqualTo("2.0");
  }
}
