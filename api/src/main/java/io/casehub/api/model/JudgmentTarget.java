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

import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import java.time.Duration;
import java.util.List;

/**
 * Binding target for caller-agnostic judgment yields.
 *
 * <p>The engine publishes a judgment request via {@code JudgmentScheduler}; any caller type (human,
 * LLM, webhook, A2A agent) can respond. Unlike {@link HumanTaskTarget}, this target carries no
 * human-specific fields (candidateGroups, outcomes, templateRef).
 *
 * <p>Refs engine#996, engine#994.
 */
public final class JudgmentTarget implements BindingTarget {

  private final String prompt;
  private final ExpressionEvaluator promptExpression;
  private final ExpressionEvaluator inputMapping;
  private final ExpressionEvaluator outputMapping;
  private final Class<?> resolutionType;
  private final Duration expiresIn;
  private final ExpressionEvaluator expiresInExpression;
  private final List<String> evidenceRequirements;

  private JudgmentTarget(Builder builder) {
    this.prompt = builder.prompt;
    this.promptExpression = builder.promptExpression;
    this.inputMapping = builder.inputMapping;
    this.outputMapping = builder.outputMapping;
    this.resolutionType = builder.resolutionType;
    this.expiresIn = builder.expiresIn;
    this.expiresInExpression = builder.expiresInExpression;
    this.evidenceRequirements =
        builder.evidenceRequirements != null
            ? List.copyOf(builder.evidenceRequirements)
            : List.of();
  }

  public static Builder builder() {
    return new Builder();
  }

  public String prompt() {
    return prompt;
  }

  public ExpressionEvaluator promptExpression() {
    return promptExpression;
  }

  public ExpressionEvaluator inputMapping() {
    return inputMapping;
  }

  public ExpressionEvaluator outputMapping() {
    return outputMapping;
  }

  public Class<?> resolutionType() {
    return resolutionType;
  }

  public Duration expiresIn() {
    return expiresIn;
  }

  public ExpressionEvaluator expiresInExpression() {
    return expiresInExpression;
  }

  public List<String> evidenceRequirements() {
    return evidenceRequirements;
  }

  public static final class Builder {

    private String prompt;
    private ExpressionEvaluator promptExpression;
    private ExpressionEvaluator inputMapping;
    private ExpressionEvaluator outputMapping;
    private Class<?> resolutionType;
    private Duration expiresIn;
    private ExpressionEvaluator expiresInExpression;
    private List<String> evidenceRequirements;

    public Builder prompt(String prompt) {
      this.prompt = prompt;
      return this;
    }

    public Builder promptExpression(String jq) {
      this.promptExpression = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder promptExpression(ExpressionEvaluator evaluator) {
      this.promptExpression = evaluator;
      return this;
    }

    public Builder inputMapping(String jq) {
      this.inputMapping = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder inputMapping(ExpressionEvaluator evaluator) {
      this.inputMapping = evaluator;
      return this;
    }

    public Builder outputMapping(String jq) {
      this.outputMapping = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder outputMapping(ExpressionEvaluator evaluator) {
      this.outputMapping = evaluator;
      return this;
    }

    public Builder resolutionType(Class<?> type) {
      this.resolutionType = type;
      return this;
    }

    public Builder expiresIn(Duration d) {
      this.expiresIn = d;
      return this;
    }

    public Builder expiresInExpression(String jq) {
      this.expiresInExpression = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder expiresInExpression(ExpressionEvaluator evaluator) {
      this.expiresInExpression = evaluator;
      return this;
    }

    public Builder evidenceRequirements(List<String> reqs) {
      this.evidenceRequirements = reqs;
      return this;
    }

    public JudgmentTarget build() {
      if (prompt == null && promptExpression == null) {
        throw new IllegalStateException("JudgmentTarget requires a prompt or a promptExpression");
      }
      if (prompt != null && promptExpression != null) {
        throw new IllegalStateException(
            "JudgmentTarget cannot specify both prompt and promptExpression");
      }
      if (expiresIn != null && expiresInExpression != null) {
        throw new IllegalStateException("cannot specify both expiresIn and expiresInExpression");
      }
      return new JudgmentTarget(this);
    }
  }
}
