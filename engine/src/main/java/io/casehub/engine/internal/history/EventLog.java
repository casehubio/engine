package io.casehub.engine.internal.history;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_log")
public class EventLog extends PanacheEntity {

  @Column(nullable = false)
  private UUID caseId;

  @Column(updatable = false, insertable = false,
          columnDefinition = "BIGINT GENERATED ALWAYS AS IDENTITY")
  @Generated(event = EventType.INSERT)
  private Long seq;

  @Column(nullable = false, length = 255)
  private CaseHubEventType eventType;

  @Column(nullable = false, length = 255)
  private EventStreamType streamType;

  /**
   * Yeap, denormalization is a thing. We want to be able to query events by workerId without having to parse the JSON metadata.
   */
  @Column(nullable = true, length = 255)
  private String workerId;

  @Column(nullable = false)
  private Instant timestamp;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private JsonNode payload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
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
}
