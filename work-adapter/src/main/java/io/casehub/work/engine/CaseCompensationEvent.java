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
package io.casehub.work.engine;

import io.casehub.platform.api.subscription.SubscribableEvent;
import java.util.Objects;
import java.util.UUID;

public record CaseCompensationEvent(
    Kind kind,
    String tenancyId,
    UUID caseId,
    String caseDefinitionName,
    String caseStatus,
    String actorId)
    implements SubscribableEvent {

  public enum Kind {
    STARTED,
    COMPLETED,
    FAULTED
  }

  private static final String TYPE_PREFIX = "io.casehub.engine.case.compensation.";

  public CaseCompensationEvent {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(tenancyId, "tenancyId");
    Objects.requireNonNull(caseId, "caseId");
  }

  @Override
  public String type() {
    return TYPE_PREFIX + kind.name().toLowerCase();
  }
}
