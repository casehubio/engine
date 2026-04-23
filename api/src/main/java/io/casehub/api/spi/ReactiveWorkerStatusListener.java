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

import io.casehub.api.model.WorkResult;
import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Reactive mirror of {@link WorkerStatusListener} — all methods return {@link Uni}.
 *
 * <p>Allows external provisioners to notify CaseHub of worker lifecycle events without blocking the
 * caller. Implementations must be idempotent — callbacks may arrive more than once.
 */
public interface ReactiveWorkerStatusListener {

  /**
   * Called when a provisioned worker has started and is ready for work.
   *
   * @param workerId the worker name/ID
   * @param sessionMeta implementation-specific metadata (e.g. tmux session ID). May be null.
   * @return a {@code Uni} completing with {@code null} on success
   */
  Uni<Void> onWorkerStarted(String workerId, Map<String, String> sessionMeta);

  /**
   * Called when a worker has completed its assigned work.
   *
   * @param workerId the worker name/ID
   * @param result the work result including status, output, and correlation key; must not be null
   * @return a {@code Uni} completing with {@code null} on success
   */
  Uni<Void> onWorkerCompleted(String workerId, WorkResult result);

  /**
   * Called when a worker has stalled. CaseEngine may reassign, retry, or escalate.
   *
   * @param workerId the worker name/ID
   * @return a {@code Uni} completing with {@code null} on success
   */
  Uni<Void> onWorkerStalled(String workerId);
}
