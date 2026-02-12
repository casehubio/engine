package io.casehub.engine.internal.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent record of a goal being reached during a CaseHub run.
 * Provides audit trail and observability of Case goal achievement.
 */
@Entity
@Table(name = "goal_reached",
       indexes = {
           @Index(name = "idx_goal_run_id", columnList = "run_id"),
           @Index(name = "idx_goal_name", columnList = "goal_name")
       })
public class GoalReached {

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
     * Name of the goal that was reached.
     */
    @Column(name = "goal_name", nullable = false, length = 255)
    private String goalName;

    /**
     * Description of the goal.
     */
    @Column(name = "goal_description", length = 1000)
    private String goalDescription;

    /**
     * Kind of goal (e.g., "success", "failure").
     */
    @Column(name = "goal_kind", length = 50)
    private String goalKind;

    /**
     * JQ condition that was evaluated to determine goal was reached.
     */
    @Column(name = "goal_condition", nullable = false, length = 2000)
    private String goalCondition;

    /**
     * Whether this goal is terminal (ends the case).
     */
    @Column(name = "terminal")
    private Boolean terminal;

    /**
     * Timestamp when the goal was reached.
     */
    @Column(name = "reached_at", nullable = false)
    private Instant reachedAt;

    /**
     * Optional: snapshot of the context when goal was reached.
     * Stored as JSONB for audit purposes.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_snapshot", columnDefinition = "jsonb")
    private JsonNode contextSnapshot;

    public GoalReached() {
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

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public String getGoalDescription() {
        return goalDescription;
    }

    public void setGoalDescription(String goalDescription) {
        this.goalDescription = goalDescription;
    }

    public String getGoalKind() {
        return goalKind;
    }

    public void setGoalKind(String goalKind) {
        this.goalKind = goalKind;
    }

    public String getGoalCondition() {
        return goalCondition;
    }

    public void setGoalCondition(String goalCondition) {
        this.goalCondition = goalCondition;
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
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
        GoalReached that = (GoalReached) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GoalReachedRecord{" +
                "id=" + id +
                ", runId=" + runId +
                ", goalName='" + goalName + '\'' +
                ", goalKind='" + goalKind + '\'' +
                ", terminal=" + terminal +
                ", reachedAt=" + reachedAt +
                '}';
    }
}
