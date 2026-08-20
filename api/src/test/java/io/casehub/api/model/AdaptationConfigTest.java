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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AdaptationConfigTest {

  @Test
  void rejectsNullTrigger() {
    assertThrows(
        NullPointerException.class, () -> new AdaptationConfig(null, "forward-replan", null, null));
  }

  @Test
  void rejectsNullRevision() {
    assertThrows(
        NullPointerException.class, () -> new AdaptationConfig("every-step", null, null, null));
  }

  @Test
  void storesFields() {
    var config = AdaptationConfig.of("every-step", "forward-replan");
    assertEquals("every-step", config.trigger());
    assertEquals("forward-replan", config.revision());
    assertNull(config.threshold());
  }

  @Test
  void caseDefinitionBuilderAcceptsAdaptationConfig() {
    var def =
        CaseDefinition.builder()
            .namespace("test")
            .name("test-case")
            .version("1.0")
            .adaptationConfig(AdaptationConfig.of("every-step", "forward-replan"))
            .build();
    assertEquals("every-step", def.getAdaptationConfig().trigger());
    assertEquals("forward-replan", def.getAdaptationConfig().revision());
  }

  @Test
  void caseDefinitionAdaptationConfigDefaultsToNull() {
    var def = CaseDefinition.builder().namespace("test").name("test-case").version("1.0").build();
    assertEquals(null, def.getAdaptationConfig());
  }

  @Test
  void storesThreshold() {
    var config = new AdaptationConfig("progress", "forward-replan", 0.3, null);
    assertEquals(0.3, config.threshold());
  }

  @Test
  void ofFactoryDefaultsThresholdToNull() {
    var config = AdaptationConfig.of("every-step", "forward-replan");
    assertNull(config.threshold());
  }

  @Test
  void rejectsNegativeThreshold() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptationConfig("progress", "forward-replan", -0.1, null));
  }

  @Test
  void rejectsThresholdAboveOne() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptationConfig("progress", "forward-replan", 1.1, null));
  }

  @Test
  void acceptsNullThreshold() {
    var config = new AdaptationConfig("every-step", "forward-replan", null, null);
    assertNull(config.threshold());
  }

  @Test
  void effectiveMetaReasoner_defaults_to_cost_ceiling() {
    var config = AdaptationConfig.of("progress", "forward-replan");
    assertEquals("cost-ceiling", config.effectiveMetaReasoner());
  }

  @Test
  void effectiveMetaReasoner_returns_explicit_value() {
    var config = new AdaptationConfig("progress", "forward-replan", null, "custom-reasoner");
    assertEquals("custom-reasoner", config.effectiveMetaReasoner());
  }

  @Test
  void metaReasoner_null_by_default() {
    var config = AdaptationConfig.of("every-step", "forward-replan");
    assertNull(config.metaReasoner());
  }
}
