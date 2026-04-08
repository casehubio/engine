package io.casehub.api.model;

import java.util.Objects;

public class Capability {

  private final String name;
  private final String inputSchema;
  private final String outputSchema;
  private String description;

  public Capability(String name, String inputSchema, String outputSchema) {
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private String inputSchema;
    private String outputSchema;
    private String description;

    private Builder() {

    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder inputSchema(String inputSchema) {
      this.inputSchema = inputSchema;
      return this;
    }

    public Builder outputSchema(String outputSchema) {
      this.outputSchema = outputSchema;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Capability build() {
      Capability capability = new Capability(Objects.requireNonNull(name), Objects.requireNonNull(inputSchema), Objects.requireNonNull(outputSchema));
      capability.setDescription(description);
      return capability;
    }
  }

}
