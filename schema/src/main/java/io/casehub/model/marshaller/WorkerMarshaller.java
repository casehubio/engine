package io.casehub.model.marshaller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.model.Worker;
import io.serverlessworkflow.api.types.Workflow;

import java.io.IOException;
import java.util.List;

public class WorkerMarshaller {

  public static class Serializer extends JsonSerializer<Worker> {
    @Override
    public void serialize(Worker value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeStartObject();

      gen.writeStringField("name", value.getName());
      if (value.getDescription() != null) {
        gen.writeStringField("description", value.getDescription());
      }
      if (value.getCapabilities() != null) {
        gen.writeObjectField("capabilities", value.getCapabilities());
      }
      if (value.getInputSchema() != null) {
        gen.writeObjectField("inputSchema", value.getInputSchema());
      }
      if (value.getOutputSchema() != null) {
        gen.writeObjectField("outputSchema", value.getOutputSchema());
      }

      // Serialize workflow field
      if (value.getWorkflow() != null) {
        if (value.isWorkflowRef()) {
          // String reference
          gen.writeStringField("workflow", value.getWorkflowAsRef());
        } else if (value.isEmbeddedWorkflow()) {
          // Workflow object - write inline fields
          // Always write at least "do" field to mark workflow presence (even if empty)
          Workflow workflow = value.getWorkflowAsEmbedded();
          if (workflow.getDocument() != null) {
            gen.writeObjectField("document", workflow.getDocument());
          }
          if (workflow.getInput() != null) {
            gen.writeObjectField("input", workflow.getInput());
          }
          if (workflow.getUse() != null) {
            gen.writeObjectField("use", workflow.getUse());
          }
          // Always write "do" field to mark embedded workflow presence
          gen.writeObjectField("do", workflow.getDo() != null ? workflow.getDo() : java.util.Collections.emptyList());
          if (workflow.getTimeout() != null) {
            gen.writeObjectField("timeout", workflow.getTimeout());
          }
          if (workflow.getOutput() != null) {
            gen.writeObjectField("output", workflow.getOutput());
          }
          if (workflow.getSchedule() != null) {
            gen.writeObjectField("schedule", workflow.getSchedule());
          }
        }
      }

      gen.writeEndObject();
    }
  }

  public static class Deserializer extends JsonDeserializer<Worker> {
    @Override
    public Worker deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      ObjectNode root = p.readValueAsTree();
      Worker worker = new Worker();
      ObjectMapper mapper = (ObjectMapper) p.getCodec();

      if (root.has("name")) {
        worker.setName(root.get("name").asText());
      }
      if (root.has("description")) {
        worker.setDescription(root.get("description").asText());
      }
      if (root.has("capabilities")) {
        List<String> capabilities = mapper.treeToValue(
            root.get("capabilities"),
            mapper.getTypeFactory().constructCollectionType(List.class, String.class));
        worker.setCapabilities(capabilities);
      }
      if (root.has("inputSchema")) {
        worker.setInputSchema(root.get("inputSchema"));
      }
      if (root.has("outputSchema")) {
        worker.setOutputSchema(root.get("outputSchema"));
      }

      // Deserialize workflow field
      JsonNode workflowNode = root.get("workflow");
      if (workflowNode != null && workflowNode.isTextual()) {
        // String reference
        worker.setWorkflow(workflowNode.asText());
      } else if (root.has("document") || root.has("do") || root.has("input") ||
                 root.has("output") || root.has("use") || root.has("schedule")) {
        // Embedded workflow - extract workflow fields
        ObjectNode workflowFields = mapper.createObjectNode();
        if (root.has("document")) workflowFields.set("document", root.get("document"));
        if (root.has("do")) workflowFields.set("do", root.get("do"));
        if (root.has("input")) workflowFields.set("input", root.get("input"));
        if (root.has("output")) workflowFields.set("output", root.get("output"));
        if (root.has("use")) workflowFields.set("use", root.get("use"));
        if (root.has("schedule")) workflowFields.set("schedule", root.get("schedule"));
        if (root.has("timeout")) workflowFields.set("timeout", root.get("timeout"));

        Workflow workflow = mapper.treeToValue(workflowFields, Workflow.class);
        worker.setWorkflow(workflow);
      }

      return worker;
    }
  }
}
