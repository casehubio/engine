package io.casehub.api.model;

import io.casehub.api.model.evaluator.ExpressionEvaluator;

public class DispatchRule {

  private final Capability capability;
  private final String name;
  private final Trigger on;
  private final ExpressionEvaluator when;

  public DispatchRule(String name, Capability capability, Trigger on, ExpressionEvaluator when) {
    this.name = name;
    this.capability = capability;
    this.on = on;
    this.when = when;
  }

  public Capability getCapability() {
    return capability;
  }

  public String getName() {
    return name;
  }

  public Trigger getOn() {
    return on;
  }

  public ExpressionEvaluator getWhen() {
    return when;
  }
}
