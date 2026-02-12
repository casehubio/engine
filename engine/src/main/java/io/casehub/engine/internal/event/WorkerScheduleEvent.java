package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseHubInstanceRunState;
import io.casehub.model.Capability;
import io.casehub.model.Worker;

public record WorkerScheduleEvent(CaseHubInstanceRunState runState,
                                  Worker worker, Capability capability) {
}
