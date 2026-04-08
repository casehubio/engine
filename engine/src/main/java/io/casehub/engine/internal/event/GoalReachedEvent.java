package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.api.model.Goal;

public record GoalReachedEvent(CaseInstance caseInstance, Goal goal) {
}
