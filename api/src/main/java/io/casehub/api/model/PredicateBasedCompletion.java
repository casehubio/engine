package io.casehub.api.model;

import io.casehub.api.model.evaluator.ExpressionEvaluator;

public class PredicateBasedCompletion implements CaseCompletion {

  private final ExpressionEvaluator doneWhen;

  public PredicateBasedCompletion(ExpressionEvaluator doneWhen) {
    this.doneWhen = doneWhen;
  }

  public ExpressionEvaluator getDoneWhen() {
    return doneWhen;
  }
}
