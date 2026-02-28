package io.casehub.engine.internal.model;

import io.casehub.context.StateContext;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

import java.util.UUID;

@Entity(name = "case_instance")
public class CaseInstance extends PanacheEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "case_definition_id", nullable = false)
  private CaseDefinition caseDefinition;

  @Column(name = "uuid", nullable = false, unique = true, updatable = false)
  private UUID uuid;

  @Transient
  private long version = 0L;

  @Transient
  private StateContext stateContext;

  private CaseState state;

  public CaseDefinition getCaseDefinition() {
    return caseDefinition;
  }

  public void setCaseDefinition(CaseDefinition caseDefinition) {
    this.caseDefinition = caseDefinition;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public StateContext getStateContext() {
    return stateContext;
  }

  public void setStateContext(StateContext stateContext) {
    this.stateContext = stateContext;
  }

  public CaseState getState() {
    return state;
  }

  public void setState(CaseState state) {
    this.state = state;
  }
}
