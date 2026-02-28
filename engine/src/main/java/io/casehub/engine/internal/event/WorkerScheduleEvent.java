package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.model.Capability;
import io.casehub.model.Worker;

public record WorkerScheduleEvent(CaseInstance caseInstance,
                                  Worker worker, Capability capability) {
}
