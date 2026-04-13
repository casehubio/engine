/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    private Builder() {}

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
      Capability capability =
          new Capability(
              Objects.requireNonNull(name),
              Objects.requireNonNull(inputSchema),
              Objects.requireNonNull(outputSchema));
      capability.setDescription(description);
      return capability;
    }
  }
}
