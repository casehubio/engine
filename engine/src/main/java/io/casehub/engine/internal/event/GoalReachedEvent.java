package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseHubInstanceRunState;
import io.casehub.model.Goal;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a Goal is reached.
 * <p>
 * Goals represent desired states or outcomes that should be achieved within a Case.
 * The Reactor evaluates goal conditions on every context change and publishes this
 * event when a goal condition evaluates to true.
 * <p>
 * Goals define what needs to be achieved and are used to track Case completion
 * and success criteria.
 */
public record GoalReachedEvent(
        CaseHubInstanceRunState runState,
        Goal goal,
        String timestamp
) {
    public GoalReachedEvent(CaseHubInstanceRunState runState, Goal goal) {
        this(runState, goal, Instant.now().toString());
    }

    public UUID getRunId() {
        return runState.getRunId();
    }
}
