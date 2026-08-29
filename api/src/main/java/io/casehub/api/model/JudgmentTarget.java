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
import java.util.Set;

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
  private final ExpressionEvaluator expiresAtExpression;
  private final List<String> evidenceRequirements;
  private final String title;
  private final ExpressionEvaluator titleExpression;
  private final Set<String> outcomes;
  private final String scope;
  private final ExpressionEvaluator scopeExpression;
  private final String priority;
  private final String verifierStrategy;
  private final String escalatorStrategy;
  private final String trustThreshold;
  private final RoutingConfig routingConfig;

  private JudgmentTarget(Builder builder) {
    this.prompt = builder.prompt;
    this.promptExpression = builder.promptExpression;
    this.inputMapping = builder.inputMapping;
    this.outputMapping = builder.outputMapping;
    this.resolutionType = builder.resolutionType;
    this.expiresIn = builder.expiresIn;
    this.expiresInExpression = builder.expiresInExpression;
    this.expiresAtExpression = builder.expiresAtExpression;
    this.evidenceRequirements =
        builder.evidenceRequirements != null
            ? List.copyOf(builder.evidenceRequirements)
            : List.of();
    this.title = builder.title;
    this.titleExpression = builder.titleExpression;
    this.outcomes = builder.outcomes != null ? Set.copyOf(builder.outcomes) : Set.of();
    this.scope = builder.scope;
    this.scopeExpression = builder.scopeExpression;
    this.priority = builder.priority;
    this.verifierStrategy = builder.verifierStrategy;
    this.escalatorStrategy = builder.escalatorStrategy;
    this.trustThreshold = builder.trustThreshold;
    this.routingConfig = builder.routingConfig;
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

  public String title() {
    return title;
  }

  public ExpressionEvaluator titleExpression() {
    return titleExpression;
  }

  public Set<String> outcomes() {
    return outcomes;
  }

  public String scope() {
    return scope;
  }

  public ExpressionEvaluator scopeExpression() {
    return scopeExpression;
  }

  public String priority() {
    return priority;
  }

  public ExpressionEvaluator expiresAtExpression() {
    return expiresAtExpression;
  }

  public String verifierStrategy() {
    return verifierStrategy;
  }

  public String escalatorStrategy() {
    return escalatorStrategy;
  }

  public String trustThreshold() {
    return trustThreshold;
  }

  public RoutingConfig routingConfig() {
    return routingConfig;
  }

  public static final class Builder {

    private String prompt;
    private ExpressionEvaluator promptExpression;
    private ExpressionEvaluator inputMapping;
    private ExpressionEvaluator outputMapping;
    private Class<?> resolutionType;
    private Duration expiresIn;
    private ExpressionEvaluator expiresInExpression;
    private ExpressionEvaluator expiresAtExpression;
    private List<String> evidenceRequirements;
    private String title;
    private ExpressionEvaluator titleExpression;
    private Set<String> outcomes;
    private String scope;
    private ExpressionEvaluator scopeExpression;
    private String priority;
    private String verifierStrategy;
    private String escalatorStrategy;
    private String trustThreshold;
    private RoutingConfig routingConfig;

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

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder titleExpression(String jq) {
      this.titleExpression = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder titleExpression(ExpressionEvaluator evaluator) {
      this.titleExpression = evaluator;
      return this;
    }

    public Builder outcomes(Set<String> outcomes) {
      this.outcomes = outcomes;
      return this;
    }

    public Builder scope(String scope) {
      this.scope = scope;
      return this;
    }

    public Builder scopeExpression(String jq) {
      this.scopeExpression = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder scopeExpression(ExpressionEvaluator evaluator) {
      this.scopeExpression = evaluator;
      return this;
    }

    public Builder priority(String priority) {
      this.priority = priority;
      return this;
    }

    public Builder expiresAtExpression(String jq) {
      this.expiresAtExpression = new JQExpressionEvaluator(jq);
      return this;
    }

    public Builder expiresAtExpression(ExpressionEvaluator evaluator) {
      this.expiresAtExpression = evaluator;
      return this;
    }

    public Builder verifierStrategy(String strategyId) {
      this.verifierStrategy = strategyId;
      return this;
    }

    public Builder escalatorStrategy(String strategyId) {
      this.escalatorStrategy = strategyId;
      return this;
    }

    public Builder trustThreshold(String threshold) {
      this.trustThreshold = threshold;
      return this;
    }

    public Builder routingConfig(RoutingConfig config) {
      this.routingConfig = config;
      return this;
    }

    public Builder human(HumanRoutingConfig config) {
      this.routingConfig = config;
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
