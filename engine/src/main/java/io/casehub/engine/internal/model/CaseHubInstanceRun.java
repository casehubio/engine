package io.casehub.engine.internal.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single execution run of a CaseHub definition.
 * Multiple runs can exist for the same CaseHub definition.
 */
@Entity
@Table(name = "casehub_instance_run")
public class CaseHubInstanceRun {

    /**
     * Primary key (database ID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Unique identifier for this specific run.
     */
    @Column(name = "run_id", nullable = false, unique = true)
    private UUID runId = UUID.randomUUID();

    /**
     * Reference to the CaseHub definition.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "casehub_id", nullable = false)
    private CaseHub caseHub;

    /**
     * The current status of this run.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CaseHubStatus status = CaseHubStatus.CREATED;

    /**
     * Timestamp when this run was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp when this run was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Timestamp when this run was started.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Timestamp when this run was completed (or failed/cancelled).
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    public CaseHubInstanceRun() {
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
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

    public CaseHub getCaseHub() {
        return caseHub;
    }

    public void setCaseHub(CaseHub caseHub) {
        this.caseHub = caseHub;
    }

    public CaseHubStatus getStatus() {
        return status;
    }

    public void setStatus(CaseHubStatus status) {
        this.status = status;

        // Auto-set timestamps based on status transitions
        if (status == CaseHubStatus.RUNNING && startedAt == null) {
            startedAt = Instant.now();
        } else if ((status == CaseHubStatus.COMPLETED ||
                    status == CaseHubStatus.FAILED ||
                    status == CaseHubStatus.CANCELLED) && completedAt == null) {
            completedAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseHubInstanceRun that = (CaseHubInstanceRun) o;
        return Objects.equals(runId, that.runId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId);
    }

    @Override
    public String toString() {
        return "CaseHubInstanceRun{" +
                "id=" + id +
                ", runId=" + runId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }

    /**
     * Status enumeration for CaseHub instance runs.
     */
    public enum CaseHubStatus {
        CREATED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
