package io.casehub.model;

public class ExecutionPolicy {

  private Integer timeoutMs = 60000; // default to 60 seconds
  private RetryPolicy retries = new RetryPolicy();

  public Integer getTimeoutMs() { return timeoutMs; }
  public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }

  public RetryPolicy getRetries() { return retries; }
  public void setRetries(RetryPolicy retries) { this.retries = retries; }
}
