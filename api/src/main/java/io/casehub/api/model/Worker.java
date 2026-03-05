package io.casehub.api.model;

import io.casehub.api.context.StateContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Worker {

    private final String name;
    private final List<Capability> capabilities;
    private final ExecutionPolicy executionPolicy;
    private final Function<StateContext, Map<String, Object>> function;
    private String description;

    public Worker(String name, List<Capability> capabilities, Function<StateContext, Map<String, Object>> function) {
        this(name, capabilities, null, function);
    }

    public Worker(String name, List<Capability> capabilities, ExecutionPolicy executionPolicy, Function<StateContext, Map<String, Object>> function) {
        this.name = name;
        this.capabilities = capabilities;
        this.executionPolicy = executionPolicy;
        this.function = function;
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

    public Function<StateContext, Map<String, Object>> getFunction() {
        return function;
    }
}
