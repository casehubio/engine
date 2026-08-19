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
package io.casehub.engine.plan.monitoring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MonitoringConfigTest {

  @Test
  void defaults_creates_enabled_config() {
    MonitoringConfig config = MonitoringConfig.defaults();
    assertTrue(config.enabled());
    assertEquals(0.5, config.perCompletionThreshold());
    assertEquals(5, config.windowSize());
  }

  @Test
  void disabled_creates_disabled_config() {
    MonitoringConfig config = MonitoringConfig.disabled();
    assertFalse(config.enabled());
  }

  @Test
  void rejects_negative_threshold() {
    assertThrows(IllegalArgumentException.class, () -> new MonitoringConfig(true, -0.1, 5));
  }

  @Test
  void rejects_threshold_above_one() {
    assertThrows(IllegalArgumentException.class, () -> new MonitoringConfig(true, 1.1, 5));
  }

  @Test
  void rejects_zero_window_size() {
    assertThrows(IllegalArgumentException.class, () -> new MonitoringConfig(true, 0.5, 0));
  }

  @Test
  void accepts_boundary_threshold_values() {
    assertDoesNotThrow(() -> new MonitoringConfig(true, 0.0, 1));
    assertDoesNotThrow(() -> new MonitoringConfig(true, 1.0, 1));
  }
}
