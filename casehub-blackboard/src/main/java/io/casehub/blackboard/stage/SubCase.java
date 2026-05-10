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
package io.casehub.blackboard.stage;

import io.casehub.api.plan.PlanElement;
import io.casehub.blackboard.strategy.DefaultSubCaseCompletionStrategy;
import io.casehub.blackboard.strategy.SubCaseCompletionStrategy;
import java.util.Objects;

/** Reference to a child case to spawn as a sub-case PlanItem. */
public class SubCase implements PlanElement {

  private final String namespace;
  private final String name;
  private final String version;
  private final SubCaseCompletionStrategy completionStrategy;

  private SubCase(Builder b) {
    this.namespace = b.namespace;
    this.name = b.name;
    this.version = b.version;
    this.completionStrategy = b.completionStrategy;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public SubCaseCompletionStrategy getCompletionStrategy() {
    return completionStrategy;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String namespace;
    private String name;
    private String version;
    private SubCaseCompletionStrategy completionStrategy = new DefaultSubCaseCompletionStrategy();

    public Builder namespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder version(String version) {
      this.version = version;
      return this;
    }

    public Builder completionStrategy(SubCaseCompletionStrategy strategy) {
      this.completionStrategy = strategy;
      return this;
    }

    public SubCase build() {
      Objects.requireNonNull(namespace, "namespace must not be null");
      Objects.requireNonNull(name, "name must not be null");
      Objects.requireNonNull(version, "version must not be null");
      return new SubCase(this);
    }
  }
}
