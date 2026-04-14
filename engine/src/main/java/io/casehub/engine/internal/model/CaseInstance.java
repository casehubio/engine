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
package io.casehub.engine.internal.model;

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.CaseStatus;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.util.UUID;

@Entity(name = "case_instance")
public class CaseInstance extends PanacheEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "case_definition_id", nullable = false)
  private CaseMetaModel caseMetaModel;

  @Column(name = "uuid", nullable = false, unique = true, updatable = false)
  private UUID uuid;

  // TODO sync version with caseContext version
  @Transient private long version = 0L;

  @Transient private CaseContext caseContext;

  @Column(name = "parent_plan_item_id", nullable = true)
  private UUID parentPlanItemId;

  @Enumerated(EnumType.STRING)
  private CaseStatus state;

  public CaseMetaModel getCaseMetaModel() {
    return caseMetaModel;
  }

  public void setCaseMetaModel(CaseMetaModel caseMetaModel) {
    this.caseMetaModel = caseMetaModel;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public CaseContext getCaseContext() {
    return caseContext;
  }

  public void setCaseContext(CaseContext caseContext) {
    this.caseContext = caseContext;
  }

  public UUID getParentPlanItemId() {
    return parentPlanItemId;
  }

  public void setParentPlanItemId(UUID parentPlanItemId) {
    this.parentPlanItemId = parentPlanItemId;
  }

  public CaseStatus getState() {
    return state;
  }

  public void setState(CaseStatus state) {
    this.state = state;
  }
}
