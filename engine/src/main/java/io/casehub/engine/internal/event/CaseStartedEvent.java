package io.casehub.engine.internal.event;

import io.casehub.context.StateContext;

import java.util.Objects;
import java.util.UUID;

/**
 * Event fired when a CaseHub instance run is started.
 * Contains the initial StateContext created by the Reactor.
 */
public record CaseStartedEvent(UUID runId, StateContext stateContext) {

    public CaseStartedEvent(UUID runId, StateContext stateContext) {
        this.runId = Objects.requireNonNull(runId, "runId cannot be null");
        this.stateContext = Objects.requireNonNull(stateContext, "stateContext cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseStartedEvent that = (CaseStartedEvent) o;
        return Objects.equals(runId, that.runId);
    }

    @Override
    public String toString() {
        return "CaseStartedEvent{" +
                "runId=" + runId +
                ", stateContextSize=" + stateContext.size() +
                '}';
    }
}
