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

import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import java.util.Objects;

public class Binding {

  private final Capability capability;
  private final String name;
  private final Trigger on;
  private ExpressionEvaluator when;
  private String conflictResolverStrategy;
  private SubCase subCase;

  public Binding(String name, Capability capability, Trigger on) {
    this.name = name;
    this.capability = capability;
    this.on = on;
  }

  public void setWhen(ExpressionEvaluator when) {
    this.when = when;
  }

  public void setConflictResolverStrategy(String conflictResolverStrategy) {
    this.conflictResolverStrategy = conflictResolverStrategy;
  }

  public Capability getCapability() {
    return capability;
  }

  public String getName() {
    return name;
  }

  public Trigger getOn() {
    return on;
  }

  public ExpressionEvaluator getWhen() {
    return when;
  }

  /**
   * Strategy name for resolving concurrent writes to the same CaseContext key. Values:
   * "LAST_WRITER_WINS" (default), "FIRST_WRITER_WINS", "FAIL". Null means use the default
   * (LAST_WRITER_WINS). See casehubio/engine#45, #51.
   */
  public String getConflictResolverStrategy() {
    return conflictResolverStrategy;
  }

  /**
   * Returns the SubCase definition for this binding, or null if this binding targets a Capability.
   * Exactly one of {@code capability} and {@code subCase} is non-null. See casehubio/engine#195.
   */
  public SubCase getSubCase() {
    return subCase;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private Capability capability;
    private Trigger on;
    private ExpressionEvaluator when;
    private String conflictResolverStrategy;
    private SubCase subCase;

    private Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder capability(Capability capability) {
      this.capability = capability;
      return this;
    }

    public Builder on(Trigger on) {
      this.on = on;
      return this;
    }

    public Builder when(ExpressionEvaluator when) {
      this.when = when;
      return this;
    }

    public Builder when(String when) {
      this.when = new JQExpressionEvaluator(when);
      return this;
    }

    public Builder conflictResolverStrategy(String conflictResolverStrategy) {
      this.conflictResolverStrategy = conflictResolverStrategy;
      return this;
    }

    public Builder subCase(SubCase subCase) {
      this.subCase = subCase;
      return this;
    }

    public Binding build() {
      Objects.requireNonNull(name);
      if (capability == null && subCase == null) {
        throw new IllegalStateException(
            "Binding '" + name + "' must have either capability or subCase");
      }
      if (capability != null && subCase != null) {
        throw new IllegalStateException(
            "Binding '" + name + "' cannot have both capability and subCase");
      }
      Objects.requireNonNull(on);
      Binding b = new Binding(name, capability, on);
      b.setWhen(when);
      b.setConflictResolverStrategy(conflictResolverStrategy);
      b.subCase = this.subCase;
      return b;
    }
  }
}
