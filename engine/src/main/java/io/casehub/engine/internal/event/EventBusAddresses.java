package io.casehub.engine.internal.event;

/**
 * EventBus addresses for CaseHub events.
 */
public final class EventBusAddresses {

    private EventBusAddresses() {
        // Utility class
    }

    /**
     * Address for case started events.
     */
    public static final String CASE_STARTED = "casehub.case.started";

    /**
     * Address for case completed events.
     */
    public static final String CASE_COMPLETED = "casehub.case.completed";

    /**
     * Address for case failed events.
     */
    public static final String CASE_FAILED = "casehub.case.failed";

    /**
     * Address for state context changed events.
     */
    public static final String CONTEXT_CHANGED = "casehub.context.changed";

    /**
     *  Worker run events (future) - published when a worker should be executed based on context changes and dispatch rules.
     */
    public static final String SCHEDULE_WORKER_RUN = "casehub.worker.schedule_run";

    /**
     * Address for worker run events - published when a worker is ready to be executed with prepared workflow and input data.
     */
    public static final String WORKER_RUN = "casehub.worker.run";

    /**
     * Address for milestone reached events - published when a milestone condition evaluates to true.
     */
    public static final String MILESTONE_REACHED = "casehub.milestone.reached";

    /**
     * Address for goal reached events - published when a goal condition evaluates to true.
     */
    public static final String GOAL_REACHED = "casehub.goal.reached";

    /**
     * Address for case completion check events - published after a goal is persisted to trigger completion condition evaluation.
     */
    public static final String CHECK_CASE_COMPLETION = "casehub.completion.check";

}
