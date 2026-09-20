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
package io.casehub.engine.trust;

import io.casehub.ledger.api.model.AttestationVerdict;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TrustEvolutionConfig(
    List<TrustEventMapping> events,
    ScoringConfig scoring,
    ConsolidationConfig consolidation,
    LevelConfig levels) {
  public TrustEvolutionConfig {
    events = List.copyOf(Objects.requireNonNull(events));
    Objects.requireNonNull(scoring);
    Objects.requireNonNull(consolidation);
    Objects.requireNonNull(levels);
  }

  public Optional<TrustEventMapping> findMapping(String actionType) {
    return events.stream().filter(m -> m.actionType().equals(actionType)).findFirst();
  }

  public record TrustEventMapping(
      String actionType, AttestationVerdict verdict, double confidence, double witnessConfidence) {
    public TrustEventMapping {
      Objects.requireNonNull(actionType);
      Objects.requireNonNull(verdict);
    }
  }

  public record ScoringConfig(int decayHalfLifeDays, double negativeDecayMultiplier) {
    public ScoringConfig {
      if (decayHalfLifeDays <= 0)
        throw new IllegalArgumentException("decayHalfLifeDays must be positive");
      if (negativeDecayMultiplier <= 0)
        throw new IllegalArgumentException("negativeDecayMultiplier must be positive");
    }
  }

  public record ConsolidationConfig(String overlayProperty, double significantChangeThreshold) {
    public ConsolidationConfig {
      Objects.requireNonNull(overlayProperty);
      if (significantChangeThreshold < 0 || significantChangeThreshold > 1)
        throw new IllegalArgumentException("significantChangeThreshold must be in [0,1]");
    }
  }

  public record LevelConfig(double high, double moderate, double low) {
    public LevelConfig {
      if (high < moderate || moderate < low)
        throw new IllegalArgumentException("levels must be high >= moderate >= low");
    }
  }
}
