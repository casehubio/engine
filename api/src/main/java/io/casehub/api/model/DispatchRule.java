package io.casehub.api.model;

public class DispatchRule {

  private final Capability capability;
  private final String name;
  private final Trigger on;
  private final String when;

  private DispatchRule(String name, Capability capability, Trigger on, String when) {
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

  public String getWhen() {
    return when;
  }
}
