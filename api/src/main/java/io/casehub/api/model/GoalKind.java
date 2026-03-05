package io.casehub.api.model;

public enum GoalKind {

  SUCCESS("success"),
  FAILURE("failure");

  private final String value;

  GoalKind(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public static GoalKind fromValue(String value) {
    for (GoalKind kind : values()) {
      if (kind.value.equals(value)) {
        return kind;
      }
    }
    throw new IllegalArgumentException("Unknown GoalKind: " + value);
  }
}
