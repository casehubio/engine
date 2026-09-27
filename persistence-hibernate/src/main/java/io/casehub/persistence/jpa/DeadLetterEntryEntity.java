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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "dead_letter_entry",
    indexes = {
      @Index(name = "idx_dle_dead_letter_id", columnList = "dead_letter_id", unique = true),
      @Index(name = "idx_dle_case_id", columnList = "case_id")
    })
public class DeadLetterEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public Long id;

  @Column(name = "dead_letter_id", nullable = false, unique = true)
  public String deadLetterId;

  @Column(name = "case_id", nullable = false)
  public UUID caseId;

  @Column(name = "worker_id", nullable = false)
  public String workerId;

  @Column(name = "idempotency_hash")
  public String idempotencyHash;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_context", columnDefinition = "jsonb")
  public String inputContext;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "retry_state", columnDefinition = "jsonb")
  public String retryState;

  @Column(name = "status", nullable = false, length = 30)
  public String status;

  @Column(name = "replay_attempts", nullable = false)
  public int replayAttempts;

  @Column(name = "arrived_at", nullable = false)
  public Instant arrivedAt;

  @Column(name = "last_replay_attempt_at")
  public Instant lastReplayAttemptAt;

  @Column(name = "tenancy_id", nullable = false, length = 64)
  public String tenancyId;
}
