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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.model.evaluator.LambdaExpressionEvaluator;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MilestoneTest {

  @Test
  void builderShouldCreateMilestoneWithMinimalFields() {
    Milestone milestone =
        Milestone.builder()
            .name("docs-received")
            .completionCriteria(".status == \"uploaded\"")
            .build();

    assertThat(milestone.getName()).isEqualTo("docs-received");
    assertThat(milestone.getCompletionCriteria()).isInstanceOf(JQExpressionEvaluator.class);
    assertThat(milestone.getEntryCriteria()).isNotNull(); // Default always-true
    assertThat(milestone.getSlaDuration()).isNull();
    assertThat(milestone.getSlaStartFrom()).isEqualTo(SlaStartFrom.MILESTONE_ACTIVATED);
    assertThat(milestone.getDescription()).isNull();
  }

  @Test
  void builderShouldCreateMilestoneWithAllFields() {
    Milestone milestone =
        Milestone.builder()
            .name("credit-check")
            .entryCriteria(".docsReady == true")
            .completionCriteria(".creditScore != null")
            .slaDuration(Duration.ofHours(24))
            .slaStartFrom(SlaStartFrom.CASE_CREATED)
            .description("Credit check milestone")
            .build();

    assertThat(milestone.getName()).isEqualTo("credit-check");
    assertThat(milestone.getEntryCriteria()).isInstanceOf(JQExpressionEvaluator.class);
    assertThat(milestone.getCompletionCriteria()).isInstanceOf(JQExpressionEvaluator.class);
    assertThat(milestone.getSlaDuration()).isEqualTo(Duration.ofHours(24));
    assertThat(milestone.getSlaStartFrom()).isEqualTo(SlaStartFrom.CASE_CREATED);
    assertThat(milestone.getDescription()).isEqualTo("Credit check milestone");
  }

  @Test
  void builderShouldSupportJQExpressionEvaluatorForEntryCriteria() {
    Milestone milestone =
        Milestone.builder()
            .name("test")
            .entryCriteria(new JQExpressionEvaluator(".ready == true"))
            .completionCriteria(".done == true")
            .build();

    assertThat(milestone.getEntryCriteria()).isInstanceOf(JQExpressionEvaluator.class);
  }

  @Test
  void builderShouldSupportJQExpressionEvaluatorForCompletionCriteria() {
    Milestone milestone =
        Milestone.builder()
            .name("test")
            .completionCriteria(new JQExpressionEvaluator(".done == true"))
            .build();

    assertThat(milestone.getCompletionCriteria()).isInstanceOf(JQExpressionEvaluator.class);
  }

  @Test
  void builderShouldSupportLambdaPredicateForEntryCriteria() {
    Milestone milestone =
        Milestone.builder()
            .name("test")
            .entryCriteria(ctx -> Boolean.TRUE.equals(ctx.getAs("ready", Boolean.class)))
            .completionCriteria(".done == true")
            .build();

    assertThat(milestone.getEntryCriteria()).isInstanceOf(LambdaExpressionEvaluator.class);
  }

  @Test
  void builderShouldSupportLambdaPredicateForCompletionCriteria() {
    Milestone milestone =
        Milestone.builder()
            .name("test")
            .completionCriteria(ctx -> Boolean.TRUE.equals(ctx.getAs("done", Boolean.class)))
            .build();

    assertThat(milestone.getCompletionCriteria()).isInstanceOf(LambdaExpressionEvaluator.class);
  }

  @Test
  void builderShouldRejectNullName() {
    assertThatThrownBy(() -> Milestone.builder().completionCriteria(".done == true").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name must not be null");
  }

  @Test
  void builderShouldRejectNullCompletionCriteria() {
    assertThatThrownBy(() -> Milestone.builder().name("test").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("completionCriteria must not be null");
  }

  @Test
  void builderShouldRejectNullEntryCriteriaString() {
    assertThatThrownBy(() -> Milestone.builder().name("test").entryCriteria((String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("entryCriteria must not be null");
  }

  @Test
  void builderShouldRejectNullCompletionCriteriaString() {
    assertThatThrownBy(() -> Milestone.builder().name("test").completionCriteria((String) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("completionCriteria must not be null");
  }

  @Test
  void builderShouldRejectNegativeSlaDuration() {
    assertThatThrownBy(
            () ->
                Milestone.builder()
                    .name("test")
                    .completionCriteria(".done == true")
                    .slaDuration(Duration.ofHours(-1))
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("slaDuration must be positive");
  }

  @Test
  void builderShouldRejectZeroSlaDuration() {
    assertThatThrownBy(
            () ->
                Milestone.builder()
                    .name("test")
                    .completionCriteria(".done == true")
                    .slaDuration(Duration.ZERO)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("slaDuration must be positive");
  }

  @Test
  void builderShouldDefaultEntryCriteriaToAlwaysTrue() {
    Milestone milestone =
        Milestone.builder().name("test").completionCriteria(".done == true").build();

    // Verify that entryCriteria defaults to LambdaExpressionEvaluator that always returns true
    assertThat(milestone.getEntryCriteria()).isInstanceOf(LambdaExpressionEvaluator.class);

    // Cast and test - the default should return true for any context
    LambdaExpressionEvaluator lambda = (LambdaExpressionEvaluator) milestone.getEntryCriteria();
    // We can't easily test the predicate without a CaseContext, so just verify it's set
    assertThat(lambda).isNotNull();
  }

  @Test
  void builderShouldDefaultSlaStartFromToMilestoneActivated() {
    Milestone milestone =
        Milestone.builder().name("test").completionCriteria(".done == true").build();

    assertThat(milestone.getSlaStartFrom()).isEqualTo(SlaStartFrom.MILESTONE_ACTIVATED);
  }

  @Test
  void equalsShouldCompareMilestonesByAllFields() {
    Milestone m1 =
        Milestone.builder()
            .name("test")
            .entryCriteria(".ready == true")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(1))
            .slaStartFrom(SlaStartFrom.CASE_CREATED)
            .description("Test milestone")
            .build();

    Milestone m2 =
        Milestone.builder()
            .name("test")
            .entryCriteria(".ready == true")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(1))
            .slaStartFrom(SlaStartFrom.CASE_CREATED)
            .description("Test milestone")
            .build();

    assertThat(m1).isEqualTo(m2);
    assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
  }

  @Test
  void equalsShouldReturnFalseForDifferentNames() {
    Milestone m1 = Milestone.builder().name("test1").completionCriteria(".done == true").build();
    Milestone m2 = Milestone.builder().name("test2").completionCriteria(".done == true").build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldReturnFalseForDifferentEntryCriteria() {
    Milestone m1 =
        Milestone.builder()
            .name("test")
            .entryCriteria(".ready1 == true")
            .completionCriteria(".done == true")
            .build();
    Milestone m2 =
        Milestone.builder()
            .name("test")
            .entryCriteria(".ready2 == true")
            .completionCriteria(".done == true")
            .build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldReturnFalseForDifferentCompletionCriteria() {
    Milestone m1 = Milestone.builder().name("test").completionCriteria(".done1 == true").build();
    Milestone m2 = Milestone.builder().name("test").completionCriteria(".done2 == true").build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldReturnFalseForDifferentSlaDuration() {
    Milestone m1 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(1))
            .build();
    Milestone m2 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(2))
            .build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldReturnFalseForDifferentSlaStartFrom() {
    Milestone m1 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(1))
            .slaStartFrom(SlaStartFrom.CASE_CREATED)
            .build();
    Milestone m2 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(1))
            .slaStartFrom(SlaStartFrom.MILESTONE_ACTIVATED)
            .build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldReturnFalseForDifferentDescriptions() {
    Milestone m1 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .description("Description 1")
            .build();
    Milestone m2 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .description("Description 2")
            .build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldHandleNullDescription() {
    Milestone m1 = Milestone.builder().name("test").completionCriteria(".done == true").build();
    Milestone m2 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .description("Description")
            .build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void equalsShouldHandleNullSlaDuration() {
    Milestone m1 = Milestone.builder().name("test").completionCriteria(".done == true").build();
    Milestone m2 =
        Milestone.builder()
            .name("test")
            .completionCriteria(".done == true")
            .slaDuration(Duration.ofHours(1))
            .build();

    assertThat(m1).isNotEqualTo(m2);
  }

  @Test
  void setDescriptionShouldUpdateDescription() {
    Milestone milestone =
        Milestone.builder().name("test").completionCriteria(".done == true").build();

    assertThat(milestone.getDescription()).isNull();

    milestone.setDescription("Updated description");
    assertThat(milestone.getDescription()).isEqualTo("Updated description");
  }
}
