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
package io.casehub.api.spi;

import io.casehub.api.model.coordination.SupervisorGateBehavior;
import java.util.UUID;

/**
 * Engine SPI for approval-gating agent actions. Implements {@link SupervisorGateBehavior} — the
 * {@link io.casehub.api.model.coordination.PlatformRole#SUPERVISOR} gating contract at the
 * worker-action level.
 *
 * <p>{@link ActionRiskClassifier} determines whether a {@link io.casehub.worker.api.PlannedAction}
 * requires gating. When gating is required, this service manages the gate lifecycle.
 */
public interface OversightGateService extends SupervisorGateBehavior {
  GateOutcome openGate(String agentId, String commitmentId, String outcome, String tenancyId);

  void fulfill(UUID gateId, String rawOutput);
}
