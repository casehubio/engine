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

import io.casehub.platform.api.subscription.SubscribableEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Subscribable event for agent-reported pathology conditions detected during case execution.
 * Enables the notification pipeline to route pathology alerts to operators and agents.
 */
public record PathologyAlertEvent(
    UUID caseId,
    String conditionType,
    String detail,
    String from,
    Instant timestamp,
    String tenancyId) implements SubscribableEvent {

  public static final String EVENT_TYPE = "pathology.alert";

  @Override
  public String type() {
    return EVENT_TYPE;
  }
}
