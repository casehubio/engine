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
package io.casehub.api.model.signal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PerceivedSignalTest {

  @Test
  void sources_exposesDepositingAgents() {
    var signal =
        new PerceivedSignal(
            "trail",
            0.8,
            3,
            "agent-c",
            Duration.ofSeconds(30),
            Set.of("agent-a", "agent-b", "agent-c"));
    assertEquals(Set.of("agent-a", "agent-b", "agent-c"), signal.sources());
  }

  @Test
  void sources_defensiveCopy() {
    var mutable = new HashSet<>(Set.of("agent-a"));
    var signal = new PerceivedSignal("trail", 0.8, 1, "agent-a", Duration.ofSeconds(30), mutable);
    mutable.add("agent-b");
    assertEquals(Set.of("agent-a"), signal.sources());
  }

  @Test
  void sources_nullDefaultsToEmpty() {
    var signal = new PerceivedSignal("trail", 0.5, 1, "agent-a", Duration.ofSeconds(10), null);
    assertEquals(Set.of(), signal.sources());
  }
}
