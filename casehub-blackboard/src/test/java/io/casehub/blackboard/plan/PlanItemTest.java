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
package io.casehub.blackboard.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for PlanItem ordering and lifecycle. See casehubio/engine#76. */
class PlanItemTest {

  @Test
  void higher_priority_sorts_before_lower() {
    PlanItem low = PlanItem.create("binding-a", "worker-a", 0);
    PlanItem high = PlanItem.create("binding-b", "worker-b", 10);
    List<PlanItem> items = new ArrayList<>(List.of(low, high));
    Collections.sort(items);
    assertThat(items.get(0).getBindingName()).isEqualTo("binding-b");
  }

  @Test
  void equal_priority_earlier_creation_sorts_first() throws InterruptedException {
    PlanItem first = PlanItem.create("binding-a", "worker-a", 5);
    Thread.sleep(2);
    PlanItem second = PlanItem.create("binding-b", "worker-b", 5);
    List<PlanItem> items = new ArrayList<>(List.of(second, first));
    Collections.sort(items);
    assertThat(items.get(0).getBindingName()).isEqualTo("binding-a");
  }

  @Test
  void default_status_is_pending() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    assertThat(item.getStatus()).isEqualTo(PlanItem.PlanItemStatus.PENDING);
  }

  @Test
  void status_transitions_pending_to_running_to_completed() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    item.setStatus(PlanItem.PlanItemStatus.RUNNING);
    assertThat(item.getStatus()).isEqualTo(PlanItem.PlanItemStatus.RUNNING);
    item.setStatus(PlanItem.PlanItemStatus.COMPLETED);
    assertThat(item.getStatus()).isEqualTo(PlanItem.PlanItemStatus.COMPLETED);
  }

  @Test
  void status_can_transition_to_faulted() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    item.setStatus(PlanItem.PlanItemStatus.FAULTED);
    assertThat(item.getStatus()).isEqualTo(PlanItem.PlanItemStatus.FAULTED);
  }

  @Test
  void status_can_transition_to_cancelled() {
    PlanItem item = PlanItem.create("binding-a", "worker-a", 0);
    item.setStatus(PlanItem.PlanItemStatus.CANCELLED);
    assertThat(item.getStatus()).isEqualTo(PlanItem.PlanItemStatus.CANCELLED);
  }
}
