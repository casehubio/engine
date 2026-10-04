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

import io.casehub.engine.common.internal.event.ActionGateApprovedEvent;
import io.casehub.engine.common.internal.event.ActionGateExpiredEvent;
import io.casehub.engine.common.internal.event.ActionGateRejectedEvent;
import io.casehub.work.api.WorkItemRef;
import io.casehub.work.api.WorkItemStatus;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.function.Consumer;

public class ActionGateCompletionApplier {

    private static final Logger                                      LOG    = Logger.getLogger(ActionGateCompletionApplier.class);
    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final Consumer<ActionGateApprovedEvent> gateApprovedPublisher;
    private final Consumer<ActionGateRejectedEvent> gateRejectedPublisher;
    private final Consumer<ActionGateExpiredEvent>  gateExpiredPublisher;

    public ActionGateCompletionApplier(
            Consumer<ActionGateApprovedEvent> gateApprovedPublisher,
            Consumer<ActionGateRejectedEvent> gateRejectedPublisher,
            Consumer<ActionGateExpiredEvent> gateExpiredPublisher) {
        this.gateApprovedPublisher = gateApprovedPublisher;
        this.gateRejectedPublisher = gateRejectedPublisher;
        this.gateExpiredPublisher  = gateExpiredPublisher;
    }

    public void apply(
            final GateRef gateRef,
            final WorkItemStatus status,
            final WorkItemRef ref,
            final String tenancyId,
            final UUID workLedgerEntryId) {
        LOG.debugf("Gate %d completion: workLedgerEntryId=%s", gateRef.gateId(), workLedgerEntryId);
        switch (status) {
            case COMPLETED -> handleApproved(gateRef, ref, tenancyId);
            case REJECTED, CANCELLED, OBSOLETE -> handleRejected(gateRef, ref, tenancyId);
            case EXPIRED, FAULTED, ESCALATED -> handleExpired(gateRef, tenancyId);
            default -> LOG.debugf(
                    "Gate WorkItem status %s for caseId=%s gateId=%d — no gate event published",
                    status, gateRef.caseId(), gateRef.gateId());
        }
    }

    public void applyGroupCompletion(
            final GateRef gateRef,
            final io.casehub.work.api.GroupStatus status,
            final String approvedBy,
            final String resolutionTypeName,
            final String tenancyId) {
        switch (status) {
            case COMPLETED -> {
                gateApprovedPublisher.accept(
                        new ActionGateApprovedEvent(
                                gateRef.caseId(),
                                tenancyId,
                                gateRef.gateId(),
                                null,
                                approvedBy,
                                resolutionTypeName));
                LOG.infof(
                        "Gate approved (group quorum): caseId=%s gateId=%d approvedBy=%s",
                        gateRef.caseId(), gateRef.gateId(), approvedBy);
            }
            case REJECTED -> {
                gateRejectedPublisher.accept(
                        new ActionGateRejectedEvent(gateRef.caseId(), tenancyId, gateRef.gateId(), null, null));
                LOG.infof(
                        "Gate rejected (group quorum failed): caseId=%s gateId=%d",
                        gateRef.caseId(), gateRef.gateId());
            }
            default -> LOG.debugf(
                    "Gate group status %s for caseId=%s gateId=%d — no event",
                    status, gateRef.caseId(), gateRef.gateId());
        }
    }

    private void handleApproved(
            final GateRef gateRef, final WorkItemRef ref, final String tenancyId) {
        final String approvedBy = resolveActorId(ref);
        gateApprovedPublisher.accept(
                new ActionGateApprovedEvent(
                        gateRef.caseId(),
                        tenancyId,
                        gateRef.gateId(),
                        ref != null ? ref.resolution() : null,
                        approvedBy,
                        ref != null ? ref.resolutionTypeName() : null));
        LOG.infof(
                "Gate approved: caseId=%s gateId=%d approvedBy=%s",
                gateRef.caseId(), gateRef.gateId(), approvedBy);
    }

    private void handleRejected(
            final GateRef gateRef, final WorkItemRef ref, final String tenancyId) {
        final String rejectedBy = resolveActorId(ref);
        gateRejectedPublisher.accept(
                new ActionGateRejectedEvent(
                        gateRef.caseId(),
                        tenancyId,
                        gateRef.gateId(),
                        ref != null ? ref.resolution() : null,
                        rejectedBy));
        LOG.infof(
                "Gate rejected: caseId=%s gateId=%d rejectedBy=%s",
                gateRef.caseId(), gateRef.gateId(), rejectedBy);
    }

    private void handleExpired(final GateRef gateRef, final String tenancyId) {
        gateExpiredPublisher.accept(
                new ActionGateExpiredEvent(gateRef.caseId(), tenancyId, gateRef.gateId()));
        LOG.infof("Gate expired: caseId=%s gateId=%d", gateRef.caseId(), gateRef.gateId());
    }

    private static String resolveActorId(final WorkItemRef ref) {
        if (ref == null) {return null;}
        if (ref.assigneeId() != null) {return ref.assigneeId();}
        if (ref.resolution() != null) {
            try {
                final com.fasterxml.jackson.databind.JsonNode node        = MAPPER.readTree(ref.resolution());
                final com.fasterxml.jackson.databind.JsonNode completedBy = node.get("completedBy");
                if (completedBy != null && !completedBy.isNull()) {return completedBy.asText();}
            } catch (final Exception e) {
                // Resolution is not JSON or completedBy is absent — return null
            }
        }
        return null;
    }
}
