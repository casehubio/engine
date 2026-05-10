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

import io.casehub.api.plan.PlanElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime lifecycle container for a {@link PlanElement}. Hierarchical — knows its parent and
 * children. Each instance starts PENDING; transitions follow PlanItemStatus rules.
 */
public class PlanItem<T extends PlanElement> {

  private final T element;
  private PlanItemStatus status;
  private PlanItem<?> parent;
  private final List<PlanItem<?>> children = new ArrayList<>();
  private Instant activatedAt;
  private Instant completedAt;

  private PlanItem(T element) {
    this.element = element;
    this.status = PlanItemStatus.PENDING;
  }

  public static <T extends PlanElement> PlanItem<T> of(T element) {
    return new PlanItem<>(element);
  }

  public T getElement() {
    return element;
  }

  public PlanItemStatus getStatus() {
    return status;
  }

  public PlanItem<?> getParent() {
    return parent;
  }

  public List<PlanItem<?>> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void addChild(PlanItem<?> child) {
    child.parent = this;
    children.add(child);
  }

  public void activate() {
    requireStatus(PlanItemStatus.PENDING, "activate");
    this.status = PlanItemStatus.ACTIVE;
    this.activatedAt = Instant.now();
  }

  public void complete() {
    requireStatus(PlanItemStatus.ACTIVE, "complete");
    this.status = PlanItemStatus.COMPLETED;
    this.completedAt = Instant.now();
  }

  public void terminate() {
    if (status.isTerminal()) return; // idempotent for terminal states
    this.status = PlanItemStatus.TERMINATED;
    this.completedAt = Instant.now();
    for (PlanItem<?> child : children) {
      child.terminate();
    }
  }

  public void fault() {
    requireStatus(PlanItemStatus.ACTIVE, "fault");
    this.status = PlanItemStatus.FAULTED;
    this.completedAt = Instant.now();
  }

  private void requireStatus(PlanItemStatus required, String operation) {
    if (status != required) {
      throw new IllegalStateException(
          "Cannot " + operation + " PlanItem in status " + status + " (requires " + required + ")");
    }
  }
}
