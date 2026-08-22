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

import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.model.evaluator.TypedMvelExpressionEvaluator;
import io.casehub.worker.api.Capability;
import org.junit.jupiter.api.Test;

class CapabilityTargetTest {

  @Test
  void oneArgConstructor_wrapsStringsInJqEvaluator() {
    var cap = Capability.of("cap", ".input", ".output");
    var ct = new CapabilityTarget(cap);
    assertThat(ct.inputProjection()).isInstanceOf(JQExpressionEvaluator.class);
    assertThat(((JQExpressionEvaluator) ct.inputProjection()).expression()).isEqualTo(".input");
    assertThat(ct.outputProjection()).isInstanceOf(JQExpressionEvaluator.class);
    assertThat(((JQExpressionEvaluator) ct.outputProjection()).expression()).isEqualTo(".output");
  }

  @Test
  void oneArgConstructor_nullOutputSchema_nullEvaluator() {
    var cap = Capability.of("cap", ".input", ".output");
    var ct = new CapabilityTarget(cap, null, null);
    assertThat(ct.inputProjection()).isNull();
    assertThat(ct.outputProjection()).isNull();
    assertThat(ct.capability()).isSameAs(cap);
  }

  @Test
  void threeArgConstructor_preservesEvaluators() {
    var cap = Capability.of("cap", ".input", ".output");
    var mvelInput = new TypedMvelExpressionEvaluator("user.name", Object.class);
    var ct = new CapabilityTarget(cap, mvelInput, null);
    assertThat(ct.inputProjection()).isSameAs(mvelInput);
    assertThat(ct.outputProjection()).isNull();
  }

  @Test
  void capability_accessorPreserved() {
    var cap = Capability.of("cap", ".input", ".output");
    var ct = new CapabilityTarget(cap);
    assertThat(ct.capability()).isSameAs(cap);
  }
}
