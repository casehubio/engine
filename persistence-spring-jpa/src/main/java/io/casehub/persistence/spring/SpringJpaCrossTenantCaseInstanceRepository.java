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
package io.casehub.persistence.spring;

import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository;
import io.casehub.persistence.jpa.CaseInstanceEntity;
import io.casehub.persistence.jpa.TenantContextManager;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SpringJpaCrossTenantCaseInstanceRepository implements CrossTenantCaseInstanceRepository {

  private final EntityManager em;
  private final TenantContextManager tcm;

  SpringJpaCrossTenantCaseInstanceRepository(EntityManager em, TenantContextManager tcm) {
    this.em = em;
    this.tcm = tcm;
  }

  @Override
  public java.util.Optional<CaseInstance> findByUuid(UUID caseId) {
    tcm.setCrossTenantContext();
    List<CaseInstanceEntity> results =
        em.createQuery(
                "SELECT ci FROM CaseInstanceEntity ci JOIN FETCH ci.caseMetaModel"
                    + " WHERE ci.uuid = :uuid",
                CaseInstanceEntity.class)
            .setParameter("uuid", caseId)
            .getResultList();
    return results.isEmpty()
        ? java.util.Optional.empty()
        : java.util.Optional.of(fromEntity(results.get(0)));
  }

  private CaseInstance fromEntity(CaseInstanceEntity entity) {
    CaseInstance instance = new CaseInstance();
    instance.id = entity.id;
    instance.tenancyId = entity.tenancyId;
    instance.setUuid(entity.uuid);
    instance.setState(entity.state);
    instance.setParentCaseId(entity.parentCaseId);
    instance.setParentPlanItemId(entity.parentPlanItemId);
    instance.setWaitingForWorkId(entity.waitingForWorkId);
    instance.setCreatedAt(entity.createdAt);
    if (entity.caseMetaModel != null) {
      CaseMetaModel m = new CaseMetaModel();
      m.id = entity.caseMetaModel.id;
      m.tenancyId = entity.caseMetaModel.tenancyId;
      m.setName(entity.caseMetaModel.name);
      m.setNamespace(entity.caseMetaModel.namespace);
      m.setVersion(entity.caseMetaModel.version);
      m.setTitle(entity.caseMetaModel.title);
      m.setDsl(entity.caseMetaModel.dsl);
      m.setDefinition(entity.caseMetaModel.definition);
      m.setCreatedAt(entity.caseMetaModel.createdAt);
      instance.setCaseMetaModel(m);
    }
    return instance;
  }
}
