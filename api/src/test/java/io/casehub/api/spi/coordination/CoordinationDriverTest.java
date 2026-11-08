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
package io.casehub.api.spi.coordination;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.coordination.CoordinationTask;
import io.casehub.api.model.coordination.CoordinatorBehavior;
import io.casehub.api.model.coordination.TopologyDescriptor;
import io.casehub.api.model.coordination.TopologyType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoordinationDriverTest {

  private static final TopologyDescriptor TOPOLOGY =
      new TopologyDescriptor(TopologyType.CENTRALISED, Map.of());
  private static final CoordinationTask TASK =
      new CoordinationTask("t-1", "analyse input", null, null);

  @Test
  void coordination_driver_extends_coordinator_behavior() {
    assertThat(CoordinatorBehavior.class).isAssignableFrom(CoordinationDriver.class);
  }
}
