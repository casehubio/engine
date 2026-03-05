package io.casehub.api.model;

public class RetryPolicy {

  private final Integer maxAttempts;
  private final Integer delayMs;

  public RetryPolicy(Integer maxAttempts, Integer delayMs) {
    this.maxAttempts = maxAttempts;
    this.delayMs = delayMs;
  }

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public Integer getDelayMs() {
    return delayMs;
  }
}
