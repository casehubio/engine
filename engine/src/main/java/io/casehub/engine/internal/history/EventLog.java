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
package io.casehub.engine.internal.history;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event_log")
public class EventLog extends PanacheEntity {

  @Column(name = "case_id", nullable = false)
  private UUID caseId;

  @Column(
      name = "seq",
      nullable = false,
      updatable = false,
      insertable = false,
      columnDefinition = "BIGINT GENERATED ALWAYS AS IDENTITY")
  @Generated(event = EventType.INSERT)
  private Long seq;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 255)
  private CaseHubEventType eventType;

  @Enumerated(EnumType.STRING)
  @Column(name = "stream_type", nullable = false, length = 255)
  private EventStreamType streamType;

  /**
   * Yeap, denormalization is a thing. We want to be able to query events by workerId without having
   * to parse the JSON metadata.
   */
  @Column(name = "worker_id", nullable = true, length = 255)
  private String workerId;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb")
  private JsonNode payload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private JsonNode metadata;

  public UUID getCaseId() {
    return caseId;
  }

  public Long getSeq() {
    return seq;
  }

  public EventStreamType getStreamType() {
    return streamType;
  }

  public String getWorkerId() {
    return workerId;
  }

  public void setCaseId(UUID caseId) {
    this.caseId = caseId;
  }

  public CaseHubEventType getEventType() {
    return eventType;
  }

  public void setEventType(CaseHubEventType eventType) {
    this.eventType = eventType;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setSeq(Long seq) {
    this.seq = seq;
  }

  public void setStreamType(EventStreamType streamType) {
    this.streamType = streamType;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public JsonNode getPayload() {
    return payload;
  }

  public void setPayload(JsonNode payload) {
    this.payload = payload;
  }

  public JsonNode getMetadata() {
    return metadata;
  }

  public void setMetadata(JsonNode metadata) {
    this.metadata = metadata;
  }

  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  public static Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId) {
    return find(
            "caseId = ?1 and workerId = ?2 and eventType in (?3, ?4, ?5)",
            caseId,
            workerId,
            CaseHubEventType.WORKER_SCHEDULED,
            CaseHubEventType.WORKER_EXECUTION_STARTED,
            CaseHubEventType.WORKER_EXECUTION_COMPLETED)
        .list();
  }

  public Uni<Long> persistScheduledEvent() {
    return persistAndFlush().map(persisted -> ((EventLog) persisted).id);
  }
}
