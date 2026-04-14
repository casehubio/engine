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

  public Binding(String name, Capability capability, Trigger on) {
    this.name = name;
    this.capability = capability;
    this.on = on;
  }

  public void setWhen(ExpressionEvaluator when) {
    this.when = when;
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private Capability capability;
    private Trigger on;
    private ExpressionEvaluator when;

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

    public Binding build() {
      Binding rule =
          new Binding(
              Objects.requireNonNull(name),
              Objects.requireNonNull(capability),
              Objects.requireNonNull(on));
      rule.setWhen(when);
      return rule;
    }
  }
}
