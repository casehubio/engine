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

/** Backoff strategy applied between retry attempts. */
public enum BackoffStrategy {

  /** Constant delay of {@code delayMs} between every attempt. */
  FIXED,

  /**
   * Delay doubles with each attempt ({@code delayMs * 2^(attempt-1)}), capped at 30 seconds.
   * Prevents thundering-herd when many workers fail simultaneously.
   */
  EXPONENTIAL,

  /**
   * Exponential backoff with random jitter in {@code [0, exponentialDelay]}, capped at 30 seconds.
   * Spreads retry load across time when many workers share the same backoff schedule.
   */
  EXPONENTIAL_WITH_JITTER
}
