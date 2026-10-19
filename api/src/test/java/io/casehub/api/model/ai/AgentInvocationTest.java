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
package io.casehub.api.model.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.smallrye.mutiny.Multi;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AgentInvocationTest {
  private AgentProvider provider(Multi<AgentEvent> events) {
    return new AgentProvider() {
      public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        return events;
      }

      public AgentSession openSession(AgentSessionInit init) {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Test
  void combinesTextAndPreservesCompletion() {
    var completion =
        new AgentEvent.InvocationComplete(3, 4, 0, 0, 0, null, 1, 0, null, 0, false, null, null);
    var events =
        Multi.createFrom()
            .<AgentEvent>items(
                new AgentEvent.TextDelta("hel"), new AgentEvent.TextDelta("lo"), completion);
    var result =
        AgentInvocation.invoke(
            provider(events), AgentSessionConfig.of("system", "user"), Duration.ofSeconds(1));
    assertEquals("hello", result.text());
    assertEquals(completion, result.completion());
  }

  @Test
  void rejectsErrorCompletion() {
    var completion =
        new AgentEvent.InvocationComplete(0, 0, 0, 0, 0, null, 1, 0, null, 0, true, null, null);
    var events = Multi.createFrom().<AgentEvent>items(completion);
    assertThrows(
        AgentException.class,
        () ->
            AgentInvocation.invoke(
                provider(events), AgentSessionConfig.of("system", "user"), Duration.ofSeconds(1)));
  }

  @Test
  void timesOutWhenStreamDoesNotComplete() {
    var events = Multi.createFrom().<AgentEvent>nothing();
    assertThrows(
        RuntimeException.class,
        () ->
            AgentInvocation.invoke(
                provider(events), AgentSessionConfig.of("system", "user"), Duration.ofMillis(50)));
  }
}
