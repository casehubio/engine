package io.casehub.engine.internal.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a CaseHub definition.
 * A CaseHub definition can have multiple execution runs (CaseHubInstanceRun).
 */
@Entity
@Table(name = "casehub", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"namespace", "name", "version"})
})
public class CaseHub {

    /**
     * Primary key (database ID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * The namespace of the CaseHub definition.
     */
    @Column(length = 255)
    private String namespace;

    /**
     * The name of the CaseHub definition.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * The semantic version of the CaseHub definition.
     */
    @Column(nullable = false, length = 50)
    private String version;

    /**
     * The title of the CaseHub definition.
     */
    @Column(length = 500)
    private String title;

    /**
     * The DSL version used by the CaseHub.
     */
    @Column(length = 50)
    private String dsl;

    /**
     * The full CaseHub definition stored as JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode definition;

    /**
     * Timestamp when this definition was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Timestamp when this definition was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * All execution runs of this CaseHub definition.
     */
    @OneToMany(mappedBy = "caseHub", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseHubInstanceRun> runs = new ArrayList<>();

    public CaseHub() {
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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDsl() {
        return dsl;
    }

    public void setDsl(String dsl) {
        this.dsl = dsl;
    }

    public JsonNode getDefinition() {
        return definition;
    }

    public void setDefinition(JsonNode definition) {
        this.definition = definition;
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

    public List<CaseHubInstanceRun> getRuns() {
        return runs;
    }

    public void setRuns(List<CaseHubInstanceRun> runs) {
        this.runs = runs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseHub caseHub = (CaseHub) o;
        return Objects.equals(namespace, caseHub.namespace) &&
                Objects.equals(name, caseHub.name) &&
                Objects.equals(version, caseHub.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name, version);
    }

    @Override
    public String toString() {
        return "CaseHub{" +
                "id=" + id +
                ", namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", title='" + title + '\'' +
                ", dsl='" + dsl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
