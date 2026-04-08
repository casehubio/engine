package io.casehub.api.model;

import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;

public class ContextChangeTrigger implements Trigger {

  private final ExpressionEvaluator filter;

  public ContextChangeTrigger(String filter) {
    this.filter = new JQExpressionEvaluator(filter);
  }

  public ContextChangeTrigger(ExpressionEvaluator filter) {
    this.filter = filter;
  }

  public ExpressionEvaluator getFilter() {
    return filter;
  }
}
