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

import io.casehub.engine.common.internal.event.ActionGateCancelledEvent;
import io.casehub.work.api.WorkItemRef;
import io.casehub.work.api.spi.WorkItemCreator;
import io.casehub.work.api.spi.WorkItemLifecycle;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.Optional;

public class ActionGateCancelledHandler {

    private static final Logger LOG = Logger.getLogger(ActionGateCancelledHandler.class);

    private final WorkItemCreator   workItemCreator;
    private final WorkItemLifecycle workItemLifecycle;

    public ActionGateCancelledHandler(
            WorkItemCreator workItemCreator,
            WorkItemLifecycle workItemLifecycle) {
        this.workItemCreator   = workItemCreator;
        this.workItemLifecycle = workItemLifecycle;
    }

    @Transactional
    public void onActionGateCancelled(final ActionGateCancelledEvent event) {
        final String                callerRef = GateRef.encode(event.caseId(), event.gateId());
        final Optional<WorkItemRef> refOpt    = workItemCreator.findByCallerRef(callerRef);

        if (refOpt.isEmpty()) {
            LOG.debugf(
                    "Gate WorkItem not found for cancellation: caseId=%s gateId=%d callerRef=%s — may have already been resolved",
                    event.caseId(), event.gateId(), callerRef);
            return;
        }

        final WorkItemRef ref = refOpt.get();
        if (ref.status() != null && ref.status().isTerminal()) {
            LOG.debugf(
                    "Gate WorkItem already terminal (status=%s): caseId=%s gateId=%d — skipping cancellation",
                    ref.status(), event.caseId(), event.gateId());
            return;
        }

        try {
            workItemLifecycle.cancel(
                    ref.id(), "casehub-engine", "Case reached terminal state while gate was pending");
            LOG.infof(
                    "Gate WorkItem cancelled (case terminated): caseId=%s gateId=%d",
                    event.caseId(), event.gateId());
        } catch (final Exception e) {
            LOG.warnf(
                    e,
                    "Failed to cancel gate WorkItem: caseId=%s gateId=%d — WorkItem may resolve against a terminated case",
                    event.caseId(),
                    event.gateId());
        }
    }
}
