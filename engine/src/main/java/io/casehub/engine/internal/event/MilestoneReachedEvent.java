package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseHubInstanceRunState;
import io.casehub.model.Milestone;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a Milestone is reached.
 * <p>
 * Milestones are observable progress markers that indicate significant
 * intermediate or final results in a Case. The Reactor evaluates milestone
 * conditions on every context change and publishes this event when a
 * milestone condition evaluates to true.
 * <p>
 * Milestones provide visibility and progress tracking, not control flow.
 */
public record MilestoneReachedEvent(
        CaseHubInstanceRunState runState,
        Milestone milestone,
        String timestamp
) {
    public MilestoneReachedEvent(CaseHubInstanceRunState runState, Milestone milestone) {
        this(runState, milestone, Instant.now().toString());
    }

    public UUID getRunId() {
        return runState.getRunId();
    }
}
