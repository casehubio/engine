package io.casehub.api.model;

public class Capability {

  private final String name;
  private final String inputSchema;
  private final String outputSchema;
  private String description;

  private Capability(String name, String inputSchema, String outputSchema) {
    this.name = name;
    this.inputSchema = inputSchema;
    this.outputSchema = outputSchema;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getInputSchema() {
    return inputSchema;
  }

  public String getOutputSchema() {
    return outputSchema;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
