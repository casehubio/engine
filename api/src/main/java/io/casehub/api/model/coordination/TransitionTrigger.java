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
package io.casehub.api.model.coordination;

import java.time.Duration;
import java.util.Objects;

public sealed interface TransitionTrigger
    permits TransitionTrigger.Threshold, TransitionTrigger.Time, TransitionTrigger.Convergence {

  record Threshold(String metric, double value) implements TransitionTrigger {
    public Threshold {
      Objects.requireNonNull(metric, "metric must not be null");
      if (metric.isBlank()) throw new IllegalArgumentException("metric must not be blank");
    }
  }

  record Time(Duration duration) implements TransitionTrigger {
    public Time {
      Objects.requireNonNull(duration, "duration must not be null");
      if (duration.isNegative() || duration.isZero())
        throw new IllegalArgumentException("duration must be positive");
    }
  }

  record Convergence(double threshold) implements TransitionTrigger {
    public Convergence {
      if (threshold < 0.0 || threshold > 1.0)
        throw new IllegalArgumentException("threshold must be in [0.0, 1.0]");
    }
  }
}
