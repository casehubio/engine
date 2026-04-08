package io.casehub.engine.internal.event;

public record WorkerRetriesExhaustedEvent(java.util.UUID caseId, String workerId, String idempotency) {
}
