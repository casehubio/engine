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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "case_queue_entry",
    indexes = {
      @Index(name = "idx_cqe_case_id", columnList = "case_id"),
      @Index(name = "idx_cqe_view_id_tenancy", columnList = "view_id, tenancy_id"),
      @Index(name = "idx_cqe_tenancy_id", columnList = "tenancy_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_cqe_case_view",
          columnNames = {"case_id", "view_id"})
    })
public class CaseQueueEntryEntity {

  @Id
  @Column(name = "id")
  public UUID id;

  @Column(name = "case_id", nullable = false)
  public UUID caseId;

  @Column(name = "tenancy_id", nullable = false, length = 64)
  public String tenancyId;

  @Column(name = "view_id", nullable = false)
  public UUID viewId;

  @Column(name = "view_name", length = 255)
  public String viewName;

  @Column(name = "status", nullable = false, length = 20)
  public String status;

  @Column(name = "assigned_to", length = 255)
  public String assignedTo;

  @Column(name = "claimed_at")
  public Instant claimedAt;

  @Column(name = "escalated_at")
  public Instant escalatedAt;

  @Column(name = "previous_view_id")
  public UUID previousViewId;

  @Column(name = "previous_view_name", length = 255)
  public String previousViewName;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
