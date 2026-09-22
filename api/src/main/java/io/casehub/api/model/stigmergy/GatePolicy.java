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
package io.casehub.api.model.stigmergy;

import jakarta.annotation.Nullable;
import java.util.Map;

public record GatePolicy(
    @Nullable Map<ImprovementStage, GateMode> modes, @Nullable Integer gateTimeoutMinutes) {

  public enum GateMode {
    GATED,
    AUTO,
    NOTIFY
  }

  public GatePolicy {
    if (modes != null) {
      for (var stage : modes.keySet()) {
        if (!stage.isGateCheckpoint()) {
          throw new IllegalArgumentException(stage + " is not a gate checkpoint");
        }
      }
    }
  }

  public GateMode effectiveMode(ImprovementStage stage) {
    if (modes != null && modes.containsKey(stage)) {
      return modes.get(stage);
    }
    return stage == ImprovementStage.PR_REVIEW ? GateMode.GATED : GateMode.AUTO;
  }

  public int effectiveGateTimeoutMinutes() {
    return gateTimeoutMinutes != null ? gateTimeoutMinutes : 1440;
  }
}
