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
package io.casehub.api.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProvisioningExceptionTest {

  @Test
  void isUncheckedException() {
    assertThat(new ProvisioningException("x")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void messageConstructor() {
    var ex = new ProvisioningException("no workers available");
    assertThat(ex.getMessage()).isEqualTo("no workers available");
  }

  @Test
  void messageCauseConstructor() {
    var cause = new RuntimeException("socket closed");
    var ex = new ProvisioningException("provisioning failed", cause);
    assertThat(ex.getMessage()).isEqualTo("provisioning failed");
    assertThat(ex.getCause()).isSameAs(cause);
  }
}
