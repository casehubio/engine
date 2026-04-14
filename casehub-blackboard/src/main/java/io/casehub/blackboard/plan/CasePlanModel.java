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
package io.casehub.blackboard.plan;

import io.casehub.blackboard.stage.Stage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Authored orchestration definition — a named list of root Stages. Separate from CaseDefinition.
 */
public class CasePlanModel {

  private final String name;
  private final List<Stage> stages;

  private CasePlanModel(Builder b) {
    this.name = b.name;
    this.stages = List.copyOf(b.stages);
  }

  public String getName() {
    return name;
  }

  public List<Stage> getStages() {
    return stages;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private final List<Stage> stages = new ArrayList<>();

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder stages(Stage... stages) {
      this.stages.addAll(Arrays.asList(stages));
      return this;
    }

    public CasePlanModel build() {
      Objects.requireNonNull(name, "name must not be null");
      return new CasePlanModel(this);
    }
  }
}
