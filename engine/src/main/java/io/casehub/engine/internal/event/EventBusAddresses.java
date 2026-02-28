package io.casehub.engine.internal.event;

/**
 * EventBus addresses for CaseHub events.
 */
public final class EventBusAddresses {

    private EventBusAddresses() {
        // Utility class
    }

    public static final String CASE_STARTED = "casehub.case.started";

    public static final String CASE_COMPLETED = "casehub.case.completed";

    public static final String CASE_FAILED = "casehub.case.failed";

    public static final String CONTEXT_CHANGED = "casehub.context.changed";

    public static final String SCHEDULE_WORKER = "casehub.worker.schedule";

    public static final String WORKER_START = "casehub.worker.start";

    public static final String WORKER_FINISHED = "casehub.worker.finished";

    public static final String MILESTONE_REACHED = "casehub.milestone.reached";

    public static final String GOAL_REACHED = "casehub.goal.reached";

    public static final String CHECK_CASE_COMPLETION = "casehub.completion.check";

}
