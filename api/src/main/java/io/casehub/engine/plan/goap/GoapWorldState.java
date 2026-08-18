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
package io.casehub.engine.plan.goap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GoapWorldState(Map<String, Condition> conditions) {

  public GoapWorldState {
    conditions = Map.copyOf(conditions);
  }

  public GoapWorldState with(String key, Condition value) {
    Map<String, Condition> copy = new HashMap<>(conditions);
    copy.put(key, value);
    return new GoapWorldState(copy);
  }

  public GoapWorldState with(String key, boolean value) {
    return with(key, Condition.fromBoolean(value));
  }

  public Condition get(String key) {
    return conditions.getOrDefault(key, Condition.UNKNOWN);
  }

  public boolean satisfies(String goalCondition) {
    return get(goalCondition) == Condition.TRUE;
  }

  public boolean satisfiesAll(Set<String> goalConditions) {
    return goalConditions.stream().allMatch(this::satisfies);
  }

  public static GoapWorldState closedWorld(Map<String, Boolean> known) {
    Map<String, Condition> conditions = new HashMap<>();
    known.forEach((k, v) -> conditions.put(k, Condition.fromBoolean(v)));
    return new GoapWorldState(conditions);
  }

  public static GoapWorldState openWorld(com.fasterxml.jackson.databind.JsonNode workingLayer) {
    Map<String, Condition> conditions = new HashMap<>();
    workingLayer
        .fieldNames()
        .forEachRemaining(
            key -> {
              com.fasterxml.jackson.databind.JsonNode value = workingLayer.get(key);
              if (value.isNull()) {
                return;
              } else if (value.isBoolean()) {
                conditions.put(key, Condition.fromBoolean(value.booleanValue()));
              } else {
                conditions.put(key, Condition.TRUE);
              }
            });
    return new GoapWorldState(conditions);
  }
}
