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
package io.casehub.persistence.memory;

import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.spi.CaseInstanceRepository;
import io.casehub.engine.spi.EventLogRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory {@link CaseInstanceRepository} for use in engine unit tests. Activated via {@code
 * quarkus.arc.selected-alternatives} — never active in production.
 */
@Alternative
@ApplicationScoped
public class InMemoryCaseInstanceRepository implements CaseInstanceRepository {

  private final AtomicLong idSeq = new AtomicLong(0);
  private final ConcurrentHashMap<UUID, CaseInstance> store = new ConcurrentHashMap<>();

  @Inject EventLogRepository eventLogRepository;

  /** Package-private setter for unit tests that cannot use CDI injection. */
  void setEventLogRepository(EventLogRepository eventLogRepository) {
    this.eventLogRepository = eventLogRepository;
  }

  @Override
  public Uni<CaseInstance> save(CaseInstance instance) {
    if (instance.id == null) {
      instance.id = idSeq.incrementAndGet();
    }
    store.put(instance.getUuid(), instance);
    return Uni.createFrom().item(instance);
  }

  @Override
  public Uni<CaseInstance> update(CaseInstance instance) {
    if (!store.containsKey(instance.getUuid())) {
      throw new IllegalStateException("CaseInstance not found for UUID: " + instance.getUuid());
    }
    store.put(instance.getUuid(), instance);
    return Uni.createFrom().item(instance);
  }

  @Override
  public Uni<CaseInstance> findByUuid(UUID uuid) {
    return Uni.createFrom().item(store.get(uuid));
  }

  @Override
  public Uni<Void> updateStateAndAppendEvent(CaseInstance instance, EventLog eventLog) {
    store.put(instance.getUuid(), instance);
    return eventLogRepository.append(eventLog);
  }
}
