package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseHubInstanceRunState;

import java.util.Objects;
import java.util.UUID;

/**
 * Event fired when StateContext is changed.
 *
 * This event is published whenever the StateContext is modified,
 * allowing participants to observe and react to state changes.
 *
 * Contains CaseHubInstanceRunState which provides access to both:
 * - The persistent CaseHubInstanceRun entity
 * - The in-memory StateContext
 */
public class StateContextChangedEvent {

    private final CaseHubInstanceRunState runState;
    private final String changeType;

    /**
     * Creates a StateContextChangedEvent.
     *
     * @param runState the current run state (includes CaseHubInstanceRun and StateContext)
     * @param changeType the type of change (e.g., "INITIALIZED", "UPDATED")
     */
    public StateContextChangedEvent(CaseHubInstanceRunState runState, String changeType) {
        this.runState = Objects.requireNonNull(runState, "runState cannot be null");
        this.changeType = Objects.requireNonNull(changeType, "changeType cannot be null");
    }

    public CaseHubInstanceRunState getRunState() {
        return runState;
    }

    public UUID getRunId() {
        return runState.getRunId();
    }

    public String getChangeType() {
        return changeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateContextChangedEvent that = (StateContextChangedEvent) o;
        return Objects.equals(runState.getRunId(), that.runState.getRunId())
            && Objects.equals(changeType, that.changeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runState.getRunId(), changeType);
    }

    @Override
    public String toString() {
        return "StateContextChangedEvent{" +
                "runId=" + runState.getRunId() +
                ", changeType='" + changeType + '\'' +
                ", status=" + runState.getStatus() +
                '}';
    }
}
