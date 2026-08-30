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

import java.util.List;
import org.junit.jupiter.api.Test;

class CallerConfigTest {

  @Test
  void humanWithCandidateGroupsAndTrust() {
    var human = new CallerConfig.Human(List.of("managers", "compliance"), "high");
    assertEquals(List.of("managers", "compliance"), human.candidateGroups());
    assertEquals("high", human.minimumTrustLevel());
  }

  @Test
  void humanWithCandidateGroupsOnly() {
    var human = new CallerConfig.Human(List.of("approvers"));
    assertEquals(List.of("approvers"), human.candidateGroups());
    assertNull(human.minimumTrustLevel());
  }

  @Test
  void humanDefensiveCopy() {
    var mutable = new java.util.ArrayList<>(List.of("a", "b"));
    var human = new CallerConfig.Human(mutable);
    mutable.add("c");
    assertEquals(2, human.candidateGroups().size());
  }

  @Test
  void llmWithModelId() {
    var llm = new CallerConfig.Llm("claude-sonnet-5");
    assertEquals("claude-sonnet-5", llm.modelId());
  }

  @Test
  void llmNoModelId() {
    var llm = new CallerConfig.Llm();
    assertNull(llm.modelId());
  }

  @Test
  void a2aWithEndpointAndSkill() {
    var a2a = new CallerConfig.A2A("https://agent.example.com", "review");
    assertEquals("https://agent.example.com", a2a.endpoint());
    assertEquals("review", a2a.skill());
  }

  @Test
  void a2aWithEndpointOnly() {
    var a2a = new CallerConfig.A2A("https://agent.example.com");
    assertEquals("https://agent.example.com", a2a.endpoint());
    assertNull(a2a.skill());
  }

  @Test
  void anyConfig() {
    var any = new CallerConfig.Any();
    assertInstanceOf(CallerConfig.class, any);
  }

  @Test
  void sealedTypeExhaustiveness() {
    CallerConfig config = new CallerConfig.Human(List.of("group"));
    String result =
        switch (config) {
          case CallerConfig.Human h -> "human:" + h.candidateGroups().size();
          case CallerConfig.Llm l -> "llm:" + l.modelId();
          case CallerConfig.A2A a -> "a2a:" + a.endpoint();
          case CallerConfig.Any a -> "any";
        };
    assertEquals("human:1", result);
  }
}
