/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.api.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Time-based trigger for scheduling worker execution. Supports cron-based periodic scheduling and
 * delay-based one-shot scheduling. Use {@link Binding#getWhen()} to add conditions for conditional
 * execution.
 */
public final class ScheduleTrigger implements Trigger {

  private final String cron;
  private final Duration delay;

  private ScheduleTrigger(String cron, Duration delay) {
    this.cron = cron;
    this.delay = delay;
  }

  /**
   * Create a cron-based periodic trigger.
   *
   * @param cronExpression Quartz cron expression
   * @return ScheduleTrigger configured for periodic execution
   */
  public static ScheduleTrigger cron(String cronExpression) {
    Objects.requireNonNull(cronExpression, "cronExpression must not be null");
    return new ScheduleTrigger(cronExpression, null);
  }

  /**
   * Create a delay-based one-shot trigger.
   *
   * @param delay Duration to wait before execution
   * @return ScheduleTrigger configured for delayed one-shot execution
   */
  public static ScheduleTrigger delay(Duration delay) {
    Objects.requireNonNull(delay, "delay must not be null");
    if (delay.isNegative() || delay.isZero()) {
      throw new IllegalArgumentException("delay must be positive");
    }
    return new ScheduleTrigger(null, delay);
  }

  /**
   * Returns the cron expression if this is a cron-based trigger.
   *
   * @return cron expression, or null if this is a delay-based trigger
   */
  public String getCron() {
    return cron;
  }

  /**
   * Returns the delay duration if this is a delay-based trigger.
   *
   * @return delay duration, or null if this is a cron-based trigger
   */
  public Duration getDelay() {
    return delay;
  }

  /**
   * Returns true if this is a cron-based periodic trigger.
   *
   * @return true if cron expression is set
   */
  public boolean isCron() {
    return cron != null;
  }

  /**
   * Returns true if this is a delay-based one-shot trigger.
   *
   * @return true if delay duration is set
   */
  public boolean isDelay() {
    return delay != null;
  }

  @Override
  public String toString() {
    if (isCron()) {
      return "ScheduleTrigger{cron='" + cron + "'}";
    } else {
      return "ScheduleTrigger{delay=" + delay + "}";
    }
  }
}
