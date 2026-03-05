package io.casehub.api.model;

import java.util.List;

public class Worker {

  private final String name;
  private final List<Capability> capabilities;
  private final ExecutionPolicy executionPolicy;
  private final Object workflow;
  private String description;

  private Worker(String name, List<Capability> capabilities,
                 ExecutionPolicy executionPolicy, Object workflow) {
    this.name = name;
    this.capabilities = capabilities;
    this.executionPolicy = executionPolicy;
    this.workflow = workflow;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Capability> getCapabilities() {
    return capabilities;
  }

  public ExecutionPolicy getExecutionPolicy() {
    return executionPolicy;
  }

  public Object getWorkflow() {
    return workflow;
  }
}
