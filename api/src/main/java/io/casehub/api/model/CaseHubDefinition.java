package io.casehub.api.model;

import java.util.ArrayList;
import java.util.List;

public class CaseHubDefinition {

  private final String namespace;
  private final String name;
  private String dsl;
  private final String version;
  private String title;
  private String summary;
  private final List<Capability> capabilities;
  private final List<Worker> workers;
  private final List<DispatchRule> rules;
  private final List<Milestone> milestones;
  private final List<Goal> goals;
  private CaseCompletion completion;

  public CaseHubDefinition(String namespace, String name, String version) {
    this.namespace = namespace;
    this.name = name;
    this.version = version;

    this.capabilities = new ArrayList<>();
    this.rules = new ArrayList<>();
    this.milestones = new ArrayList<>();
    this.goals = new ArrayList<>();
    this.workers = new ArrayList<>();
  }

  public String getVersion() {
    return version;
  }

  public String getDsl() {
    return dsl;
  }

  public void setDsl(String dsl) {
    this.dsl = dsl;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }

  public List<Worker> getWorkers() {
    return workers;
  }

  public List<DispatchRule> getRules() {
    return rules;
  }

  public List<Milestone> getMilestones() {
    return milestones;
  }

  public List<Goal> getGoals() {
    return goals;
  }

  public CaseCompletion getCompletion() {
    return completion;
  }

  public void setCompletion(CaseCompletion completion) {
    this.completion = completion;
  }
}
