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

import io.casehub.api.model.improvement.ImprovementOutcome;
import io.casehub.engine.common.spi.Resettable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImprovementOutcomeStore implements Resettable {

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<ImprovementOutcome>> byCategory =
      new ConcurrentHashMap<>();

  public void record(ImprovementOutcome outcome) {
    byCategory.computeIfAbsent(outcome.category(), k -> new CopyOnWriteArrayList<>()).add(outcome);
  }

  public List<ImprovementOutcome> findByCategory(String category, int limit) {
    var list = byCategory.get(category);
    if (list == null || list.isEmpty()) return List.of();
    int start = Math.max(0, list.size() - limit);
    return List.copyOf(list.subList(start, list.size()));
  }

  public double failureRate(String category, String target) {
    var list = byCategory.get(category);
    if (list == null || list.isEmpty()) return 0.0;

    var matching = list.stream().filter(o -> target.equals(o.target())).toList();
    if (matching.isEmpty()) return 0.0;

    long failures =
        matching.stream()
            .filter(
                o ->
                    o.status() == ImprovementOutcome.OutcomeStatus.FAILED
                        || o.status() == ImprovementOutcome.OutcomeStatus.REJECTED
                        || o.status() == ImprovementOutcome.OutcomeStatus.REGRESSION)
            .count();

    return (double) failures / matching.size();
  }

  @Override
  public void reset() {
    byCategory.clear();
  }
}
