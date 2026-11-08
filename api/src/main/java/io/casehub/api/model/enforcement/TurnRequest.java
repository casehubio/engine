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
package io.casehub.api.model.enforcement;

import io.casehub.api.model.coordination.CommunicationMode;
import java.util.Objects;

public record TurnRequest(String agentId, String actionType, CommunicationMode communicationMode) {

  public TurnRequest {
    Objects.requireNonNull(agentId, "agentId must not be null");
    if (agentId.isBlank()) throw new IllegalArgumentException("agentId must not be blank");
    Objects.requireNonNull(actionType, "actionType must not be null");
    Objects.requireNonNull(communicationMode, "communicationMode must not be null");
  }
}
