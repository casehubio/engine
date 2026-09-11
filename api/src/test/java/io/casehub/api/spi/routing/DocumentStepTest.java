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
package io.casehub.api.spi.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentStepTest {

  @Test
  void constructsWithAllFields() {
    var step =
        new DocumentStep(
            "Isolate mailbox",
            "Access to Exchange admin",
            "Mailbox isolated",
            "exchange:disable-mailbox");
    assertThat(step.description()).isEqualTo("Isolate mailbox");
    assertThat(step.preconditions()).isEqualTo("Access to Exchange admin");
    assertThat(step.expectedOutcome()).isEqualTo("Mailbox isolated");
    assertThat(step.automationHint()).isEqualTo("exchange:disable-mailbox");
  }

  @Test
  void nullableFieldsDefaultToNull() {
    var step = new DocumentStep("Reset credentials", null, null, null);
    assertThat(step.description()).isEqualTo("Reset credentials");
    assertThat(step.preconditions()).isNull();
    assertThat(step.expectedOutcome()).isNull();
    assertThat(step.automationHint()).isNull();
  }
}
