package io.casehub.engine.internal.event;

import io.casehub.engine.internal.model.CaseInstance;

import java.util.Objects;

/**
 * Event fired when a CaseHub instance run is started.
 * Contains the initial StateContext created by the Reactor.
 */
public record CaseStartedEvent(CaseInstance instance) {

  public CaseStartedEvent(CaseInstance instance) {
    this.instance = Objects.requireNonNull(instance, "instance cannot be null");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseStartedEvent that = (CaseStartedEvent) o;
    return Objects.equals(instance, that.instance);
  }

  @Override
  public String toString() {
    return "CaseStartedEvent{" +
            "uuid=" + instance.getUuid() +
            '}';
  }
}
