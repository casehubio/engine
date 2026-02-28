package io.casehub.engine.internal.event;

public record GoalReachedEvent(io.casehub.engine.internal.model.CaseInstance caseInstance, io.casehub.model.Goal goal) {
}
