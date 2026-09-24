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
package io.casehub.engine.internal.recovery;

import io.casehub.api.model.CaseStatus;
import io.casehub.api.spi.event.EventDispatcher;
import io.casehub.engine.common.internal.event.CaseStatusChanged;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.internal.engine.CaseCompletionTracker;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Administrative recovery operations for cases in terminal states. Provides the {@link
 * #unfault(UUID)} operation that transitions a FAULTED case back to RUNNING, enabling DLQ replay
 * and resumed execution.
 */
public class CaseRecoveryService {

  private static final Logger LOG = Logger.getLogger(CaseRecoveryService.class);

  private final CaseInstanceCache caseInstanceCache;
  private final CrossTenantCaseInstanceRepository caseInstanceRepository;
  private final CaseCompletionTracker caseCompletionTracker;
  private final EventDispatcher eventDispatcher;

  public CaseRecoveryService(
      CaseInstanceCache caseInstanceCache,
      CrossTenantCaseInstanceRepository caseInstanceRepository,
      CaseCompletionTracker caseCompletionTracker,
      EventDispatcher eventDispatcher) {
    this.caseInstanceCache = caseInstanceCache;
    this.caseInstanceRepository = caseInstanceRepository;
    this.caseCompletionTracker = caseCompletionTracker;
    this.eventDispatcher = eventDispatcher;
  }

  /**
   * Transitions a FAULTED case back to RUNNING. Returns the case instance on success, empty if the
   * case is not found or not FAULTED.
   *
   * <p>The state is set synchronously before returning. An async {@link CaseStatusChanged} event is
   * dispatched to persist the transition and re-evaluate bindings. The completion tracker is
   * re-registered so the case can be awaited again.
   */
  public Optional<CaseInstance> unfault(UUID caseId) {
    CaseInstance instance = caseInstanceCache.get(caseId);
    if (instance == null) {
      instance = caseInstanceRepository.findByUuid(caseId).orElse(null);
    }
    if (instance == null) {
      LOG.warnf("Unfault: case not found: %s", caseId);
      return Optional.empty();
    }
    if (instance.getState() != CaseStatus.FAULTED) {
      LOG.warnf("Unfault: case %s is %s, not FAULTED", caseId, instance.getState());
      return Optional.empty();
    }

    String oldStatus = instance.getState().name();
    instance.setState(CaseStatus.RUNNING);

    caseCompletionTracker.remove(caseId);
    caseCompletionTracker.register(caseId);

    eventDispatcher.dispatch(new CaseStatusChanged(instance, oldStatus, CaseStatus.RUNNING.name()));

    LOG.infof("Case unfaulted: caseId=%s (%s → RUNNING)", caseId, oldStatus);
    return Optional.of(instance);
  }
}
