package io.casehub.engine.internal.worker;

import io.casehub.api.context.StateContext;
import io.serverlessworkflow.api.types.Workflow;

import java.util.Map;
import java.util.function.Function;

public class WorkflowFunction implements Function<StateContext, Map<String, Object>> {

  private final Workflow workflow;

  public WorkflowFunction(Workflow workflow) {
    this.workflow = workflow;
  }

  public Workflow getWorkflow() {
    return workflow;
  }

  @Override
  public Map<String, Object> apply(StateContext ctx) {
    throw new UnsupportedOperationException("Use WorkflowExecutor for WorkflowFunction");
  }
}
