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
package io.casehub.engine.internal.improvement;

import io.casehub.api.model.stigmergy.ConductorDecision;
import io.casehub.api.model.stigmergy.ConductorInboxEntry;
import io.casehub.api.model.stigmergy.ConductorInboxEntry.Status;
import io.casehub.api.model.stigmergy.WatchPattern;
import io.casehub.engine.common.spi.Resettable;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class ConductorInboxManager implements Resettable {

  private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, ConductorInboxEntry>> entries =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<WatchPattern>> watchPatterns =
      new ConcurrentHashMap<>();

  public String enqueue(UUID caseId, ConductorInboxEntry entry) {
    entries.computeIfAbsent(caseId, k -> new ConcurrentHashMap<>()).put(entry.id(), entry);
    return entry.id();
  }

  public List<ConductorInboxEntry> pending(UUID caseId) {
    var caseEntries = entries.get(caseId);
    if (caseEntries == null) {
      return List.of();
    }
    return caseEntries.values().stream().filter(e -> e.status() == Status.PENDING).toList();
  }

  public int pendingCount(UUID caseId) {
    var caseEntries = entries.get(caseId);
    if (caseEntries == null) {
      return 0;
    }
    return (int) caseEntries.values().stream().filter(e -> e.status() == Status.PENDING).count();
  }

  public List<ConductorInboxEntry> allEntries(UUID caseId) {
    var caseEntries = entries.get(caseId);
    if (caseEntries == null) {
      return List.of();
    }
    return List.copyOf(caseEntries.values());
  }

  public void resolve(UUID caseId, String entryId, ConductorDecision decision) {
    var caseEntries = entries.get(caseId);
    if (caseEntries == null) {
      return;
    }
    var existing = caseEntries.get(entryId);
    if (existing == null) {
      return;
    }
    var resolved =
        new ConductorInboxEntry(
            existing.caseId(),
            existing.id(),
            existing.stage(),
            decision.outcome(),
            existing.category(),
            existing.areaId(),
            existing.improvementCaseId(),
            existing.summary(),
            existing.escalationTriggers(),
            existing.confidence(),
            existing.queuedAt(),
            Instant.now(),
            existing.timeoutMinutes(),
            decision);
    caseEntries.put(entryId, resolved);
  }

  public List<WatchPattern> activeWatchPatterns(UUID caseId) {
    var patterns = watchPatterns.get(caseId);
    if (patterns == null) {
      return List.of();
    }
    return List.copyOf(patterns);
  }

  public void addWatchPattern(UUID caseId, WatchPattern pattern) {
    watchPatterns.computeIfAbsent(caseId, k -> new CopyOnWriteArrayList<>()).add(pattern);
  }

  public void removeWatchPattern(UUID caseId, String patternId) {
    var patterns = watchPatterns.get(caseId);
    if (patterns != null) {
      patterns.removeIf(p -> p.id().equals(patternId));
    }
  }

  @Override
  public void reset() {
    entries.clear();
    watchPatterns.clear();
  }
}
