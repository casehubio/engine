package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;

public record CaseStatusChanged(CaseInstance instance, String oldStatus, String newStatus) {
}
