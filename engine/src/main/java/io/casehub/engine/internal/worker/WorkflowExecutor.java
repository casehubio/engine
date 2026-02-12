package io.casehub.engine.internal.worker;

import io.serverlessworkflow.api.types.Workflow;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.WorkflowModel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class WorkflowExecutor {

  public CompletableFuture<WorkflowModel> execute(Workflow workflow, Map<String, Object> inputData) {
    try (WorkflowApplication app = WorkflowApplication.builder().build()) {
      return app.workflowDefinition(workflow).instance(inputData).start();
    } catch (Exception e) {
      return CompletableFuture.failedFuture(new RuntimeException("Workflow execution failed", e));
    }
  }

}
