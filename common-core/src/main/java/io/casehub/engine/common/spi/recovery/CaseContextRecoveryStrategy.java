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
package io.casehub.engine.common.spi.recovery;

import io.casehub.api.context.CaseContext;
import io.casehub.engine.common.internal.model.CaseInstance;

/**
 * Strategy for recovering {@link CaseContext} on cache miss and participating in context-change
 * propagation. Part of the blackboard propagation architecture — implementations react to context
 * mutations within the persistence transaction.
 *
 * <p>Two built-in implementations:
 *
 * <ul>
 *   <li>{@code SnapshotRecoveryStrategy} (default) — O(1) recovery from a persisted context
 *       snapshot
 *   <li>{@code EventLogReplayRecoveryStrategy} (experimental) — O(N) recovery by replaying the
 *       event log
 * </ul>
 *
 * @see WorkerExecutionRecoveryService
 */
public interface CaseContextRecoveryStrategy {

  /**
   * Recover the {@link CaseContext} for a case instance on cache miss. Called by {@link
   * WorkerExecutionRecoveryService#loadOrRestoreCaseInstance} after loading the instance from the
   * repository.
   *
   * @param instance the loaded case instance (may contain a persisted context snapshot)
   * @return the recovered context
   */
  CaseContext recover(CaseInstance instance);

  /**
   * Called within the persistence transaction when context has changed. Part of the blackboard
   * propagation cycle — the snapshot strategy persists here; the event-log strategy is a no-op.
   *
   * @param instance the case instance whose context changed
   * @param context the current context state
   */
  void onContextChanged(CaseInstance instance, CaseContext context);
}
