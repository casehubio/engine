package io.casehub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.casehub.model.marshaller.WorkerMarshaller;
import io.serverlessworkflow.api.types.Workflow;

import java.util.List;

@JsonSerialize(using = WorkerMarshaller.Serializer.class)
@JsonDeserialize(using = WorkerMarshaller.Deserializer.class)
public class Worker {

  // PENDING → PROCESSING → COMPLETED → FAILED

  private String name;
  private String description;
  private List<String> capabilities;
  private JsonNode inputSchema;
  private JsonNode outputSchema;

  private ExecutionPolicy executionPolicy = new ExecutionPolicy();

  /**
   * Workflow definition: either String (path to external workflow file) OR Workflow object (embedded).
   * Type: String | Workflow
   */
  private Object workflow;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public List<String> getCapabilities() { return capabilities; }
  public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

  public JsonNode getInputSchema() { return inputSchema; }
  public void setInputSchema(JsonNode inputSchema) { this.inputSchema = inputSchema; }

  public JsonNode getOutputSchema() { return outputSchema; }
  public void setOutputSchema(JsonNode outputSchema) { this.outputSchema = outputSchema; }

  public ExecutionPolicy getExecutionPolicy() { return executionPolicy; }
  public void setExecutionPolicy(ExecutionPolicy executionPolicy) { this.executionPolicy = executionPolicy; }

  public Object getWorkflow() { return workflow; }
  public void setWorkflow(Object workflow) { this.workflow = workflow; }

  public boolean isWorkflowRef() {
    return workflow instanceof String;
  }

  public boolean isEmbeddedWorkflow() {
    return workflow instanceof Workflow;
  }

  public String getWorkflowAsRef() {
    return (String) workflow;
  }

  public Workflow getWorkflowAsEmbedded() {
    return (Workflow) workflow;
  }
}
