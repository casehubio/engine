package io.casehub.api.model;

import io.casehub.api.context.StateContext;
import io.casehub.api.model.holder.WorkerFunctionHolder;
import io.serverlessworkflow.api.types.Workflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Worker {

  private final String name;
  private final List<Capability> capabilities;
  private final WorkerFunctionHolder<?> functionHolder;
  private ExecutionPolicy executionPolicy;
  private String description;

  public Worker(String name, List<Capability> capabilities, Function<StateContext, Map<String, Object>> function) {
    this(name, capabilities, new WorkerFunctionHolder<>(function));
  }

  public Worker(String name, List<Capability> capabilities, Workflow workflow) {
    this(name, capabilities, new WorkerFunctionHolder<>(workflow));
  }

  public Worker(String name, List<Capability> capabilities, File file) {
    this(name, capabilities, new WorkerFunctionHolder<>(file));
  }

  private Worker(String name, List<Capability> capabilities, WorkerFunctionHolder<?> functionHolder) {
    this.name = name;
    this.capabilities = capabilities;
    this.functionHolder = functionHolder;
  }


  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public void setExecutionPolicy(ExecutionPolicy executionPolicy) {
    this.executionPolicy = executionPolicy;
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

  public WorkerFunctionHolder<?> getFunction() {
    return functionHolder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private List<Capability> capabilities;
    private WorkerFunctionHolder<?> functionHolder;
    private ExecutionPolicy executionPolicy;
    private String description;

    private Builder() {

    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder capabilities(Capability... capabilities) {
      this.capabilities = Arrays.asList(capabilities);
      return this;
    }

    public Builder capabilities(List<Capability> capabilities) {
      this.capabilities = capabilities;
      return this;
    }

    public Builder function(Function<StateContext, Map<String, Object>> function) {
      this.functionHolder = new WorkerFunctionHolder<>(function);
      return this;
    }

    public Builder function(Workflow workflow) {
      this.functionHolder = new WorkerFunctionHolder<>(workflow);
      return this;
    }

    public Builder function(File file) {
      this.functionHolder = new WorkerFunctionHolder<>(file);
      return this;
    }

    public Builder executionPolicy(ExecutionPolicy executionPolicy) {
      this.executionPolicy = executionPolicy;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Worker build() {
      Worker worker = new Worker(
              Objects.requireNonNull(name),
              Objects.requireNonNull(capabilities),
              Objects.requireNonNull(functionHolder)
      );
      worker.setExecutionPolicy(executionPolicy);
      worker.setDescription(description);
      return worker;
    }
  }
}
