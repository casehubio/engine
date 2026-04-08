package io.casehub.api.model;

//TODO this must be replaced by a more generic implementation that can support multiple goals of different kinds, not just success and failure
public class GoalBasedCompletion implements CaseCompletion {

  private final GoalExpression success;
  private final GoalExpression failure;

  public GoalBasedCompletion(GoalExpression success, GoalExpression failure) {
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
