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
 * Intrinsic behavioral contracts — the platform implements specific behavior for these roles.
 *
 * @see AgentRole
 * @see StructuralRole
 */
public enum PlatformRole {
  /**
   * Orchestrates, delegates tasks, allocates agents, and aggregates results.
   *
   * @see CoordinatorBehavior
   * @see io.casehub.api.spi.coordination.CoordinationDriver
   */
  COORDINATOR(StructuralRole.INITIATOR),

  /**
   * Oversees, approves/rejects, gates progression. Implemented at two granularities via {@link
   * SupervisorGateBehavior}:
   *
   * <ul>
   *   <li>{@link io.casehub.api.spi.OversightGateService} — worker-action gates for {@code
   *       PlannedAction} approval
   *   <li>{@code ConductorInboxManager} — evolution-pipeline gates for research stage approval
   * </ul>
   *
   * <p>Classification (deciding <em>whether</em> gating is needed) is handled by {@link
   * io.casehub.api.spi.ActionRiskClassifier}, which precedes the gate.
   */
  SUPERVISOR(StructuralRole.INITIATOR),

  /**
   * Evaluates, scores, and renders decisions on agent outputs. Coordinates multi-judge voting
   * protocols (majority, consensus, weighted, threshold) via {@link JudgeBehavior}.
   *
   * <p>The behavioral contract is defined by {@link
   * io.casehub.api.spi.evaluation.EvaluationProtocol}, which provides:
   *
   * <ul>
   *   <li>Single-subject evaluation against criteria
   *   <li>Multi-candidate scoring and ranking
   *   <li>Decision rendering with voting strategy
   * </ul>
   *
   * @see JudgeBehavior
   * @see io.casehub.api.spi.evaluation.EvaluationProtocol
   */
  JUDGE(StructuralRole.PEER),
  MODERATOR(StructuralRole.PEER);

  private final StructuralRole requiredStructuralRole;

  PlatformRole(final StructuralRole requiredStructuralRole) {
    this.requiredStructuralRole = requiredStructuralRole;
  }

  public StructuralRole requiredStructuralRole() {
    return requiredStructuralRole;
  }
}
