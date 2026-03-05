package io.casehub.api.model;

public class GoalBasedCompletion implements CaseCompletion {

  private final GoalExpression success;
  private final GoalExpression failure;

  private GoalBasedCompletion(GoalExpression success, GoalExpression failure) {
    this.success = success;
    this.failure = failure;
  }

  public GoalExpression getSuccess() {
    return success;
  }

  public GoalExpression getFailure() {
    return failure;
  }
}
