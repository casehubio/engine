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
package io.casehub.engine.runtime.spring.broadcast;

import io.casehub.api.view.CaseStreamEventView;
import io.casehub.engine.common.spi.event.CaseContextUpdatedEvent;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CaseStreamSpringBroadcaster {

  private record ActiveStream(UUID caseId, SubmissionPublisher<CaseStreamEventView> publisher) {}

  private final CopyOnWriteArrayList<ActiveStream> streams = new CopyOnWriteArrayList<>();

  @EventListener
  public void onPlanItemChanged(PlanItemStateChangedEvent event) {
    var view =
        new CaseStreamEventView(
            event.caseId(),
            "plan-item",
            Map.of(
                "planItemId", event.planItemId(),
                "bindingName", event.bindingName(),
                "previousStatus",
                    event.previousStatus() != null ? event.previousStatus().name() : "NONE",
                "newStatus", event.newStatus().name()));
    dispatch(event.caseId(), view);
  }

  @EventListener
  public void onContextUpdated(CaseContextUpdatedEvent event) {
    var view =
        new CaseStreamEventView(
            event.caseId(), "context", Map.of("changedLayer", event.changedLayer()));
    dispatch(event.caseId(), view);
  }

  public Flow.Publisher<CaseStreamEventView> stream(UUID caseId) {
    var publisher = new SubmissionPublisher<CaseStreamEventView>();
    streams.add(new ActiveStream(caseId, publisher));
    return publisher;
  }

  private void dispatch(UUID caseId, CaseStreamEventView view) {
    streams.removeIf(s -> s.publisher().getNumberOfSubscribers() == 0 && s.publisher().isClosed());
    for (var active : streams) {
      if (caseId.equals(active.caseId())) {
        if (active.publisher().getNumberOfSubscribers() == 0) {
          streams.remove(active);
        } else {
          active.publisher().offer(view, (subscriber, dropped) -> false);
        }
      }
    }
  }
}
