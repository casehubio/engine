package io.casehub.engine.internal.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "case_meta_model", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"namespace", "name", "version"})
})
public class CaseMetaModel extends PanacheEntity {

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 255)
  private String namespace;

  @Column(nullable = false, length = 50)
  private String version;

  @Column(length = 500)
  private String title;

  @Column(length = 50)
  private String dsl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private JsonNode definition;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "caseMetaModel", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CaseInstance> caseInstance = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
  }

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

  public List<CaseInstance> getCaseInstance() {
    return caseInstance;
  }

  public void setCaseInstance(List<CaseInstance> caseInstance) {
    this.caseInstance = caseInstance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseMetaModel caseHub = (CaseMetaModel) o;
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
            '}';
  }
}
