package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.api.model.Milestone;

import java.util.Objects;

public record MilestoneReachedEvent(CaseInstance caseInstance,
                                    Milestone milestone) {

  public MilestoneReachedEvent(CaseInstance caseInstance,
                               Milestone milestone) {
    this.caseInstance = Objects.requireNonNull(caseInstance, "Instance cannot be null");
    this.milestone = Objects.requireNonNull(milestone, "Milestone cannot be null");
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MilestoneReachedEvent that)) return false;
    return Objects.equals(milestone, that.milestone) && Objects.equals(caseInstance, that.caseInstance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(caseInstance, milestone);
  }
}
