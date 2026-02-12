package io.casehub.engine.internal.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent record of a milestone being reached during a CaseHub run.
 * Provides audit trail and observability of Case progress.
 */
@Entity
@Table(name = "milestone_reached",
       indexes = {
           @Index(name = "idx_milestone_run_id", columnList = "run_id"),
           @Index(name = "idx_milestone_name", columnList = "milestone_name")
       })
public class MilestoneReached {

    /**
     * Primary key (database ID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Reference to the CaseHub run.
     */
    @Column(name = "run_id", nullable = false)
    private UUID runId;

    /**
     * Name of the milestone that was reached.
     */
    @Column(name = "milestone_name", nullable = false, length = 255)
    private String milestoneName;

    /**
     * Description of the milestone.
     */
    @Column(name = "milestone_description", length = 1000)
    private String milestoneDescription;

    /**
     * JQ condition that was evaluated to determine milestone was reached.
     */
    @Column(name = "milestone_condition", nullable = false, length = 2000)
    private String milestoneCondition;

    /**
     * Timestamp when the milestone was reached.
     */
    @Column(name = "reached_at", nullable = false)
    private Instant reachedAt;

    /**
     * Optional: snapshot of the context when milestone was reached.
     * Stored as JSONB for audit purposes.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_snapshot", columnDefinition = "jsonb")
    private JsonNode contextSnapshot;

    public MilestoneReached() {
    }

    @PrePersist
    public void prePersist() {
        if (reachedAt == null) {
            reachedAt = Instant.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getRunId() {
        return runId;
    }

    public void setRunId(UUID runId) {
        this.runId = runId;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    public String getMilestoneDescription() {
        return milestoneDescription;
    }

    public void setMilestoneDescription(String milestoneDescription) {
        this.milestoneDescription = milestoneDescription;
    }

    public String getMilestoneCondition() {
        return milestoneCondition;
    }

    public void setMilestoneCondition(String milestoneCondition) {
        this.milestoneCondition = milestoneCondition;
    }

    public Instant getReachedAt() {
        return reachedAt;
    }

    public void setReachedAt(Instant reachedAt) {
        this.reachedAt = reachedAt;
    }

    public JsonNode getContextSnapshot() {
        return contextSnapshot;
    }

    public void setContextSnapshot(JsonNode contextSnapshot) {
        this.contextSnapshot = contextSnapshot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MilestoneReached that = (MilestoneReached) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MilestoneReachedRecord{" +
                "id=" + id +
                ", runId=" + runId +
                ", milestoneName='" + milestoneName + '\'' +
                ", reachedAt=" + reachedAt +
                '}';
    }
}
