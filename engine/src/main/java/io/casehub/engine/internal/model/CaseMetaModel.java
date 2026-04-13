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

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "case_meta_model",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"namespace", "name", "version"})})
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
    return Objects.equals(namespace, caseHub.namespace)
        && Objects.equals(name, caseHub.name)
        && Objects.equals(version, caseHub.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(namespace, name, version);
  }

  @Override
  public String toString() {
    return "CaseHub{"
        + "id="
        + id
        + ", namespace='"
        + namespace
        + '\''
        + ", name='"
        + name
        + '\''
        + ", version='"
        + version
        + '\''
        + ", title='"
        + title
        + '\''
        + ", dsl='"
        + dsl
        + '\''
        + ", createdAt="
        + createdAt
        + '}';
  }
}
