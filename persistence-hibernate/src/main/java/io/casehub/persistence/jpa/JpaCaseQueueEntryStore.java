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

import io.casehub.engine.queue.model.CaseQueueEntry;
import io.casehub.engine.queue.model.QueueEntryStatus;
import io.casehub.engine.queue.spi.CaseQueueEntryStore;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Alternative
@Priority(2)
@ApplicationScoped
public class JpaCaseQueueEntryStore implements CaseQueueEntryStore {

  private final EntityManager em;
  private final TenantContextManager tcm;

  @Inject
  JpaCaseQueueEntryStore(EntityManager em, TenantContextManager tcm) {
    this.em = em;
    this.tcm = tcm;
  }

  @Override
  @Transactional
  public CaseQueueEntry save(CaseQueueEntry entry) {
    CaseQueueEntryEntity e = toEntity(entry);
    em.merge(e);
    return entry;
  }

  @Override
  @Transactional
  public CaseQueueEntry upsertByCaseAndView(CaseQueueEntry entry) {
    List<CaseQueueEntryEntity> existing =
        em.createQuery(
                "SELECT e FROM CaseQueueEntryEntity e WHERE e.caseId = :caseId AND e.viewId = :viewId",
                CaseQueueEntryEntity.class)
            .setParameter("caseId", entry.getCaseId())
            .setParameter("viewId", entry.getViewId())
            .getResultList();
    if (!existing.isEmpty()) {
      CaseQueueEntryEntity ex = existing.get(0);
      ex.viewName = entry.getViewName();
      ex.tenancyId = entry.getTenancyId();
      em.merge(ex);
      return toModel(ex);
    }
    return save(entry);
  }

  @Override
  @Transactional
  public Optional<CaseQueueEntry> findById(UUID id) {
    CaseQueueEntryEntity e = em.find(CaseQueueEntryEntity.class, id);
    return Optional.ofNullable(e).map(this::toModel);
  }

  @Override
  @Transactional
  public Optional<CaseQueueEntry> findByCaseAndView(UUID caseId, UUID viewId) {
    return em
        .createQuery(
            "SELECT e FROM CaseQueueEntryEntity e WHERE e.caseId = :caseId AND e.viewId = :viewId",
            CaseQueueEntryEntity.class)
        .setParameter("caseId", caseId)
        .setParameter("viewId", viewId)
        .getResultList()
        .stream()
        .findFirst()
        .map(this::toModel);
  }

  @Override
  @Transactional
  public List<CaseQueueEntry> findByView(UUID viewId, String tenancyId) {
    tcm.setTenantContext(tenancyId);
    return em
        .createQuery(
            "SELECT e FROM CaseQueueEntryEntity e WHERE e.viewId = :viewId AND e.tenancyId = :tenancyId",
            CaseQueueEntryEntity.class)
        .setParameter("viewId", viewId)
        .setParameter("tenancyId", tenancyId)
        .getResultList()
        .stream()
        .map(this::toModel)
        .toList();
  }

  @Override
  @Transactional
  public List<CaseQueueEntry> findByCaseId(UUID caseId) {
    return em
        .createQuery(
            "SELECT e FROM CaseQueueEntryEntity e WHERE e.caseId = :caseId",
            CaseQueueEntryEntity.class)
        .setParameter("caseId", caseId)
        .getResultList()
        .stream()
        .map(this::toModel)
        .toList();
  }

  @Override
  @Transactional
  public long countByView(UUID viewId, String tenancyId) {
    tcm.setTenantContext(tenancyId);
    return em.createQuery(
            "SELECT COUNT(e) FROM CaseQueueEntryEntity e WHERE e.viewId = :viewId AND e.tenancyId = :tenancyId",
            Long.class)
        .setParameter("viewId", viewId)
        .setParameter("tenancyId", tenancyId)
        .getSingleResult();
  }

  @Override
  @Transactional
  public boolean delete(UUID id) {
    CaseQueueEntryEntity e = em.find(CaseQueueEntryEntity.class, id);
    if (e == null) {
      return false;
    }
    em.remove(e);
    return true;
  }

  @Override
  @Transactional
  public void deleteByCaseId(UUID caseId) {
    em.createQuery("DELETE FROM CaseQueueEntryEntity e WHERE e.caseId = :caseId")
        .setParameter("caseId", caseId)
        .executeUpdate();
  }

  @Override
  @Transactional
  public Optional<CaseQueueEntry> claimIfPending(UUID entryId, String userId) {
    CaseQueueEntryEntity e =
        em.find(CaseQueueEntryEntity.class, entryId, LockModeType.PESSIMISTIC_WRITE);
    if (e == null || !QueueEntryStatus.PENDING.name().equals(e.status)) {
      return Optional.empty();
    }
    e.status = QueueEntryStatus.CLAIMED.name();
    e.assignedTo = userId;
    e.claimedAt = Instant.now();
    em.merge(e);
    return Optional.of(toModel(e));
  }

  private CaseQueueEntryEntity toEntity(CaseQueueEntry m) {
    CaseQueueEntryEntity e = new CaseQueueEntryEntity();
    e.id = m.getId();
    e.caseId = m.getCaseId();
    e.tenancyId = m.getTenancyId();
    e.viewId = m.getViewId();
    e.viewName = m.getViewName();
    e.status = m.getStatus().name();
    e.assignedTo = m.getAssignedTo();
    e.claimedAt = m.getClaimedAt();
    e.escalatedAt = m.getEscalatedAt();
    e.previousViewId = m.getPreviousViewId();
    e.previousViewName = m.getPreviousViewName();
    e.createdAt = m.getCreatedAt();
    return e;
  }

  private CaseQueueEntry toModel(CaseQueueEntryEntity e) {
    CaseQueueEntry m =
        new CaseQueueEntry(
            e.id,
            e.caseId,
            e.tenancyId,
            e.viewId,
            e.viewName,
            QueueEntryStatus.valueOf(e.status),
            e.createdAt);
    m.setAssignedTo(e.assignedTo);
    m.setClaimedAt(e.claimedAt);
    m.setEscalatedAt(e.escalatedAt);
    m.setPreviousViewId(e.previousViewId);
    m.setPreviousViewName(e.previousViewName);
    return m;
  }
}
