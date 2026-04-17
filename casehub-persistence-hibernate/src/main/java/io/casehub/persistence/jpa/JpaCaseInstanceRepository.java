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
package io.casehub.persistence.jpa;

import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.engine.spi.CaseInstanceRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class JpaCaseInstanceRepository implements CaseInstanceRepository {

  @Override
  public Uni<CaseInstance> save(CaseInstance instance) {
    return Panache.withTransaction(
        () ->
            Panache.getSession()
                .chain(
                    session -> {
                      CaseInstanceEntity entity = new CaseInstanceEntity();
                      entity.uuid = instance.getUuid();
                      entity.state = instance.getState();
                      entity.parentPlanItemId = instance.getParentPlanItemId();
                      if (instance.getCaseMetaModel() != null) {
                        entity.caseMetaModel =
                            session.getReference(
                                CaseMetaModelEntity.class, instance.getCaseMetaModel().getId());
                      }
                      return entity
                          .persist()
                          .map(
                              v -> {
                                instance.id = entity.id;
                                return instance;
                              });
                    }));
  }

  @Override
  public Uni<CaseInstance> update(CaseInstance instance) {
    return Panache.withTransaction(
        () ->
            CaseInstanceEntity.<CaseInstanceEntity>findById(instance.id)
                .invoke(
                    entity -> {
                      entity.state = instance.getState();
                      entity.parentPlanItemId = instance.getParentPlanItemId();
                    })
                .replaceWith(instance));
  }

  @Override
  public Uni<CaseInstance> findByUuid(UUID uuid) {
    return Panache.withSession(
            () ->
                CaseInstanceEntity.<CaseInstanceEntity>find(
                        "from CaseInstanceEntity ci join fetch ci.caseMetaModel where ci.uuid = ?1",
                        uuid)
                    .firstResult())
        .map(entity -> entity == null ? null : fromEntity(entity));
  }

  @Override
  public Uni<Void> updateStateAndAppendEvent(CaseInstance instance, EventLog eventLog) {
    EventLogEntity logEntity = new EventLogEntity();
    logEntity.caseId = eventLog.getCaseId();
    logEntity.eventType = eventLog.getEventType();
    logEntity.streamType = eventLog.getStreamType();
    logEntity.workerId = eventLog.getWorkerId();
    logEntity.timestamp = eventLog.getTimestamp();
    logEntity.payload = eventLog.getPayload();
    logEntity.metadata = eventLog.getMetadata();

    return Panache.withTransaction(
            () ->
                CaseInstanceEntity.<CaseInstanceEntity>findById(instance.id)
                    .chain(
                        entity -> {
                          entity.state = instance.getState();
                          entity.parentPlanItemId = instance.getParentPlanItemId();
                          return Panache.getSession().chain(s -> s.merge(entity));
                        })
                    .chain(merged -> logEntity.persistAndFlush()))
        .invoke(
            () -> {
              eventLog.id = logEntity.id;
              eventLog.setSeq(logEntity.seq);
            })
        .replaceWithVoid();
  }

  private CaseInstance fromEntity(CaseInstanceEntity entity) {
    CaseInstance instance = new CaseInstance();
    instance.id = entity.id;
    instance.setUuid(entity.uuid);
    instance.setState(entity.state);
    instance.setParentPlanItemId(entity.parentPlanItemId);
    if (entity.caseMetaModel != null) {
      instance.setCaseMetaModel(fromMetaEntity(entity.caseMetaModel));
    }
    return instance;
  }

  private CaseMetaModel fromMetaEntity(CaseMetaModelEntity entity) {
    CaseMetaModel m = new CaseMetaModel();
    m.setId(entity.id);
    m.setName(entity.name);
    m.setNamespace(entity.namespace);
    m.setVersion(entity.version);
    m.setTitle(entity.title);
    m.setDsl(entity.dsl);
    m.setDefinition(entity.definition);
    m.setCreatedAt(entity.createdAt);
    return m;
  }
}
