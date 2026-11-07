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
package io.casehub.api.model.coordination;

/**
 * Marker for components that implement {@link PlatformRole#SUPERVISOR} gating behavior — the
 * ability to pause execution, wait for approval, and resume with a decision.
 *
 * <p>Implementors operate at different granularities:
 *
 * <ul>
 *   <li>{@link io.casehub.api.spi.OversightGateService} — worker-action level, gates individual
 *       {@code PlannedAction}s
 *   <li>{@code ConductorInboxManager} — evolution-pipeline level, gates research pipeline stages
 * </ul>
 *
 * <p>Classification components that decide <em>whether</em> gating is needed (such as {@link
 * io.casehub.api.spi.ActionRiskClassifier}) are not gate implementations and do not carry this
 * marker. They are the triage step that precedes the gate.
 *
 * @see PlatformRole#SUPERVISOR
 */
public interface SupervisorGateBehavior {}
