package io.casehub.api.model.holder;

public class WorkerFunctionHolder<T> {

  private T value;

  public WorkerFunctionHolder(T value) {
    this.value = value;
  }

  public T getValue() {
    return value;
  }
}
