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
package io.casehub.api.spi.judgment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CallerIdentityTest {

  @Test
  void ofFactory() {
    var id = CallerIdentity.of("user-42", "human");
    assertEquals("user-42", id.callerId());
    assertEquals("human", id.callerType());
  }

  @Test
  void anonymousFactory() {
    var id = CallerIdentity.anonymous();
    assertNull(id.callerId());
    assertNull(id.callerType());
  }

  @Test
  void nullableFields() {
    var id = CallerIdentity.of(null, "llm");
    assertNull(id.callerId());
    assertEquals("llm", id.callerType());
  }
}
