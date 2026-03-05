package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.api.model.Worker;

import java.util.Map;

public record WorkflowExecutionCompleted(CaseInstance caseInstance,
                                         Worker worker,
                                         String idempotency,
                                         Map<String, Object> output) {
}
