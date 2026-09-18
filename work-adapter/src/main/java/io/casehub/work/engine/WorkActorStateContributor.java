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
package io.casehub.work.engine;

import io.casehub.platform.api.actor.ActorStateAccumulator;
import io.casehub.platform.api.actor.ActorStateContributor;
import io.casehub.work.api.WorkItem;
import io.casehub.work.api.WorkItemQuery;
import io.casehub.work.api.WorkItemStatus;
import io.casehub.work.api.spi.WorkItemStore;
import java.util.List;
import java.util.UUID;

public class WorkActorStateContributor implements ActorStateContributor {

  private final WorkItemStore workItemStore;

  public WorkActorStateContributor(final WorkItemStore workItemStore) {
    this.workItemStore = workItemStore;
  }

  @Override
  public String sourceName() {
    return "work";
  }

  @Override
  public void contribute(final String actorId, final ActorStateAccumulator acc) {
    final List<WorkItem> items =
        workItemStore.scan(
            WorkItemQuery.builder()
                .assigneeId(actorId)
                .statusIn(
                    List.of(
                        WorkItemStatus.ASSIGNED,
                        WorkItemStatus.IN_PROGRESS,
                        WorkItemStatus.SUSPENDED))
                .build());

    for (final WorkItem wi : items) {
      final CallerRef ref = CallerRef.parse(wi.callerRef());
      final UUID caseId = ref != null ? ref.caseId() : null;
      acc.workItem(
          wi.id(), wi.title(), wi.status() != null ? wi.status().name() : null, null, caseId);
    }
  }
}
