package io.casehub.api.model;

public class ExecutionPolicy {

  private final Integer timeoutMs;
  private final RetryPolicy retries;

  private ExecutionPolicy(Integer timeoutMs, RetryPolicy retries) {
    this.timeoutMs = timeoutMs;
    this.retries = retries;
  }

  public Integer getTimeoutMs() {
    return timeoutMs;
  }

  public RetryPolicy getRetries() {
    return retries;
  }
}
