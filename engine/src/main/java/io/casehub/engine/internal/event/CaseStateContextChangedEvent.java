package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;

import java.util.Objects;

public record CaseStateContextChangedEvent(CaseInstance instance) {

  public CaseStateContextChangedEvent(CaseInstance instance) {
    this.instance = Objects.requireNonNull(instance, "instance cannot be null");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseStateContextChangedEvent that = (CaseStateContextChangedEvent) o;
    return Objects.equals(instance, that.instance);
  }

  @Override
  public String toString() {
    return "CaseStateContextChangedEvent{" +
            "uuid=" + instance.getUuid() +
            '}';
  }
}
