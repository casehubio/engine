package io.casehub.model;

public class RetryPolicy {

  private Integer maxAttempts = 3;
  private Integer delayMs = 100;

  public Integer getMaxAttempts() { return maxAttempts; }
  public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

  public Integer getDelayMs() { return delayMs; }
  public void setDelayMs(Integer delayMs) { this.delayMs = delayMs; }
}
