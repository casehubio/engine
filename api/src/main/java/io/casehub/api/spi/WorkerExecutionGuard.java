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

import java.util.UUID;

/**
 * SPI: determines whether a worker is allowed to execute for a given case.
 *
 * <p>The engine's default implementation ({@code AllowAllWorkerExecutionGuard}) always permits
 * execution. The {@code casehub-resilience} module provides an {@code @Alternative} implementation
 * that blocks quarantined workers (PoisonPill detection).
 *
 * <p>Implementations are CDI beans. Override via {@code @Alternative @Priority}.
 */
public interface WorkerExecutionGuard {

  /**
   * Returns {@code true} if the worker is blocked and must NOT be scheduled.
   *
   * @param workerId the worker name
   * @param caseId the case instance ID
   */
  boolean isBlocked(String workerId, UUID caseId);
}
