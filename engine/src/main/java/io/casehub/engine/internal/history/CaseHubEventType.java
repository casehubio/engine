package io.casehub.engine.internal.history;

public enum CaseHubEventType {
    CASE_STARTED,
    CASE_COMPLETED,
    CASE_FAILED,
    CASE_CANCELLED,

    TASK_CREATED,
    TASK_COMPLETED,
    TASK_FAILED,
    TASK_CANCELLED,
    WORKER_SCHEDULED
}
