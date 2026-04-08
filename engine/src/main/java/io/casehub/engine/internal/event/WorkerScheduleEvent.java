package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.api.model.Capability;
import io.casehub.api.model.Worker;

public record WorkerScheduleEvent(CaseInstance caseInstance,
                                  Worker worker, Capability capability) {
}
