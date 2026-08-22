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
package io.casehub.engine.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PortfolioConfigTest {

  @Test
  void defaultsUsesGoapThenLlm() {
    var config = PortfolioConfig.defaults();
    assertEquals(List.of("goap", "llm"), config.delegates());
  }

  @Test
  void defaultTimeouts() {
    var config = PortfolioConfig.defaults();
    assertEquals(1000L, config.timeoutFor("goap"));
    assertEquals(30000L, config.timeoutFor("llm"));
  }

  @Test
  void customDelegatesPreserved() {
    var config = new PortfolioConfig(List.of("llm"), Map.of("llm", 5000L));
    assertEquals(List.of("llm"), config.delegates());
    assertEquals(5000L, config.timeoutFor("llm"));
  }

  @Test
  void unknownStrategyReturnsDefaultTimeout() {
    var config = PortfolioConfig.defaults();
    assertEquals(PortfolioConfig.DEFAULT_TIMEOUT_MS, config.timeoutFor("unknown"));
  }

  @Test
  void nullDelegatesDefaultsToGoapLlm() {
    var config = new PortfolioConfig(null, null);
    assertEquals(List.of("goap", "llm"), config.delegates());
  }

  @Test
  void emptyDelegatesDefaultsToGoapLlm() {
    var config = new PortfolioConfig(List.of(), null);
    assertEquals(List.of("goap", "llm"), config.delegates());
  }

  @Test
  void nonPositiveTimeoutRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PortfolioConfig(List.of("goap"), Map.of("goap", 0L)));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PortfolioConfig(List.of("goap"), Map.of("goap", -100L)));
  }

  @Test
  void caseDefinitionBuilderAcceptsPortfolioConfig() {
    var def =
        io.casehub.api.model.CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0")
            .portfolioConfig(PortfolioConfig.defaults())
            .build();
    assertNotNull(def.getPortfolioConfig());
    assertEquals(List.of("goap", "llm"), def.getPortfolioConfig().delegates());
  }

  @Test
  void caseDefinitionPortfolioConfigDefaultsToNull() {
    var def =
        io.casehub.api.model.CaseDefinition.builder()
            .namespace("test")
            .name("test")
            .version("1.0")
            .build();
    assertNull(def.getPortfolioConfig());
  }
}
