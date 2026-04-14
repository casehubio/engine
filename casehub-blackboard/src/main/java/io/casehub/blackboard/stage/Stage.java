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

import io.casehub.api.context.CaseContext;
import io.casehub.api.model.Worker;
import io.casehub.api.model.evaluator.ExpressionEvaluator;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.model.evaluator.LambdaExpressionEvaluator;
import io.casehub.api.plan.PlanElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** CMMN Stage — a named logical grouping of Workers with entry/exit criteria. */
public class Stage implements PlanElement {

  private final String name;
  private final ExpressionEvaluator entryCondition;
  private final ExpressionEvaluator exitCondition;
  private final List<Worker> workers;
  private final List<Stage> nestedStages;
  private final List<SubCase> subCases;

  private Stage(Builder b) {
    this.name = b.name;
    this.entryCondition = b.entryCondition;
    this.exitCondition = b.exitCondition;
    this.workers = List.copyOf(b.workers);
    this.nestedStages = List.copyOf(b.nestedStages);
    this.subCases = List.copyOf(b.subCases);
  }

  public String getName() {
    return name;
  }

  public ExpressionEvaluator getEntryCondition() {
    return entryCondition;
  }

  public ExpressionEvaluator getExitCondition() {
    return exitCondition;
  }

  public List<Worker> getWorkers() {
    return workers;
  }

  public List<Stage> getNestedStages() {
    return nestedStages;
  }

  public List<SubCase> getSubCases() {
    return subCases;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private ExpressionEvaluator entryCondition;
    private ExpressionEvaluator exitCondition;
    private final List<Worker> workers = new ArrayList<>();
    private final List<Stage> nestedStages = new ArrayList<>();
    private final List<SubCase> subCases = new ArrayList<>();

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder entry(String jq) {
      this.entryCondition =
          new JQExpressionEvaluator(Objects.requireNonNull(jq, "entry condition must not be null"));
      return this;
    }

    public Builder entry(Predicate<CaseContext> predicate) {
      this.entryCondition =
          new LambdaExpressionEvaluator(
              Objects.requireNonNull(predicate, "entry condition must not be null"));
      return this;
    }

    public Builder entry(ExpressionEvaluator evaluator) {
      this.entryCondition = evaluator;
      return this;
    }

    public Builder exit(String jq) {
      this.exitCondition =
          new JQExpressionEvaluator(Objects.requireNonNull(jq, "exit condition must not be null"));
      return this;
    }

    public Builder exit(Predicate<CaseContext> predicate) {
      this.exitCondition =
          new LambdaExpressionEvaluator(
              Objects.requireNonNull(predicate, "exit condition must not be null"));
      return this;
    }

    public Builder exit(ExpressionEvaluator evaluator) {
      this.exitCondition = evaluator;
      return this;
    }

    public Builder workers(Worker... workers) {
      this.workers.addAll(Arrays.asList(workers));
      return this;
    }

    public Builder nestedStage(Stage stage) {
      this.nestedStages.add(stage);
      return this;
    }

    public Builder subCase(SubCase subCase) {
      this.subCases.add(subCase);
      return this;
    }

    public Stage build() {
      Objects.requireNonNull(name, "name must not be null");
      Objects.requireNonNull(entryCondition, "entry condition must not be null");
      return new Stage(this);
    }
  }
}
