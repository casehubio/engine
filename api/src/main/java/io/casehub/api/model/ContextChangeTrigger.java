package io.casehub.api.model;

public class ContextChangeTrigger implements Trigger {

  private final String filter;

  private ContextChangeTrigger(String filter) {
    this.filter = filter;
  }

  public String getFilter() {
    return filter;
  }
}
