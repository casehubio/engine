package io.casehub.api.model;

import io.casehub.api.model.evaluator.ExpressionEvaluator;

public class ContextChangeTrigger implements Trigger {

  private final ExpressionEvaluator filter;

  public ContextChangeTrigger(ExpressionEvaluator filter) {
    this.filter = filter;
  }

  public ExpressionEvaluator getFilter() {
    return filter;
  }
}
