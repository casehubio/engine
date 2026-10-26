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
package io.casehub.engine.runtime.improvement;

import io.casehub.api.model.improvement.ImprovementConfig;
import io.casehub.api.model.stigmergy.TickTrace.TickTrigger;
import io.casehub.engine.common.spi.Resettable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

public class EvolutionTimerBackstop implements Resettable {

  private static final Logger LOG = Logger.getLogger(EvolutionTimerBackstop.class);

  private record RegisteredCase(
      UUID caseId, String tenancyId, ImprovementConfig config, Instant lastTick) {}

  private final EvolutionTicker ticker;
  private final ConcurrentHashMap<UUID, RegisteredCase> registered = new ConcurrentHashMap<>();

  public EvolutionTimerBackstop(EvolutionTicker ticker) {
    this.ticker = ticker;
  }

  public EvolutionTimerBackstop() {
    this.ticker = null;
  }

  public void register(UUID caseId, String tenancyId, ImprovementConfig config) {
    if (ticker == null) return;
    registered.put(caseId, new RegisteredCase(caseId, tenancyId, config, Instant.now()));
  }

  public void unregister(UUID caseId) {
    registered.remove(caseId);
  }

  public void sweep() {
    if (ticker == null) return;
    var now = Instant.now();
    for (var entry : registered.entrySet()) {
      var rc = entry.getValue();
      var interval = Duration.ofMinutes(rc.config().effectiveEvolutionTickIntervalMinutes());
      if (Duration.between(rc.lastTick(), now).compareTo(interval) >= 0) {
        try {
          ticker.tick(rc.caseId(), rc.tenancyId(), rc.config(), TickTrigger.TIMER);
          registered.put(
              rc.caseId(), new RegisteredCase(rc.caseId(), rc.tenancyId(), rc.config(), now));
        } catch (Exception e) {
          LOG.warnf(e, "Timer backstop tick failed for caseId=%s", rc.caseId());
        }
      }
    }
  }

  public int registeredCount() {
    return registered.size();
  }

  @Override
  public void reset() {
    registered.clear();
  }
}
