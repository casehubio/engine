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
package io.casehub.engine.internal.improvement;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.stigmergy.ImprovementBudget;
import io.casehub.api.model.stigmergy.ImprovementRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DenyPatternDynamicTest {

  private static final String TENANT = "test-tenant";
  private ImprovementBudgetEnforcer enforcer;
  private UUID caseId;

  @BeforeEach
  void setUp() {
    enforcer = new ImprovementBudgetEnforcer(new InMemoryDenyPatternStore());
    caseId = UUID.randomUUID();
  }

  @Test
  void addDynamicPatternBlocksMatchingPath() {
    enforcer.addDenyPattern(caseId, "auth-service", TENANT);
    var request = makeRequest(List.of("src/main/java/auth-service/AuthProvider.java"));

    assertThat(enforcer.isDenied(caseId, request, TENANT)).isTrue();
  }

  @Test
  void removeDynamicPatternUnblocksPath() {
    enforcer.addDenyPattern(caseId, "auth-service", TENANT);
    enforcer.removeDenyPattern(caseId, "auth-service", TENANT);
    var request = makeRequest(List.of("src/main/java/auth-service/AuthProvider.java"));

    assertThat(enforcer.isDenied(caseId, request, TENANT)).isFalse();
  }

  @Test
  void staticPatternsCannotBeRemovedDynamically() {
    enforcer.removeDenyPattern(caseId, "EvolutionTicker", TENANT);
    var request =
        makeRequest(
            List.of("src/main/java/io/casehub/engine/internal/improvement/EvolutionTicker.java"));

    assertThat(enforcer.isDenied(caseId, request, TENANT)).isTrue();
  }

  @Test
  void effectiveDenySetIsStaticUnionDynamic() {
    enforcer.addDenyPattern(caseId, "custom-deny", TENANT);

    var staticRequest =
        makeRequest(List.of("src/main/java/io/casehub/api/model/stigmergy/ImprovementConfig.java"));
    assertThat(enforcer.isDenied(caseId, staticRequest, TENANT)).isTrue();

    var dynamicRequest = makeRequest(List.of("src/main/java/custom-deny/Foo.java"));
    assertThat(enforcer.isDenied(caseId, dynamicRequest, TENANT)).isTrue();

    var allowedRequest = makeRequest(List.of("src/main/java/other/Bar.java"));
    assertThat(enforcer.isDenied(caseId, allowedRequest, TENANT)).isFalse();
  }

  @Test
  void perCaseIsolation() {
    var case2 = UUID.randomUUID();
    enforcer.addDenyPattern(caseId, "my-pattern", TENANT);
    var request = makeRequest(List.of("src/my-pattern/Foo.java"));

    assertThat(enforcer.isDenied(caseId, request, TENANT)).isTrue();
    assertThat(enforcer.isDenied(case2, request, TENANT)).isFalse();
  }

  @Test
  void checkMethodIncludesDynamicDenyPatterns() {
    enforcer.addDenyPattern(caseId, "dynamic-blocked", TENANT);
    var budget = new ImprovementBudget(null, null, null, null, null, null, null);
    var request = makeRequest(List.of("src/dynamic-blocked/Foo.java"));

    var result = enforcer.check(caseId, budget, request, TENANT);

    assertThat(result).isInstanceOf(ImprovementBudgetEnforcer.BudgetCheck.Denied.class);
    assertThat(((ImprovementBudgetEnforcer.BudgetCheck.Denied) result).reason())
        .contains("dynamic deny pattern");
  }

  @Test
  void dynamicDenyPatternsReturnsAddedPatterns() {
    enforcer.addDenyPattern(caseId, "pattern-a", TENANT);
    enforcer.addDenyPattern(caseId, "pattern-b", TENANT);

    assertThat(enforcer.dynamicDenyPatterns(caseId, TENANT))
        .containsExactlyInAnyOrder("pattern-a", "pattern-b");
  }

  @Test
  void dynamicDenyPatternsReturnsEmptyForUnknownCase() {
    assertThat(enforcer.dynamicDenyPatterns(UUID.randomUUID(), TENANT)).isEmpty();
  }

  @Test
  void staticDenyPatternsExposesStructuralSet() {
    assertThat(ImprovementBudgetEnforcer.staticDenyPatterns())
        .contains("EvolutionTicker", "ImprovementBudget", "ImprovementConfig");
  }

  @Test
  void duplicateAddIsIdempotent() {
    enforcer.addDenyPattern(caseId, "same-pattern", TENANT);
    enforcer.addDenyPattern(caseId, "same-pattern", TENANT);

    assertThat(enforcer.dynamicDenyPatterns(caseId, TENANT)).hasSize(1);
  }

  @Test
  void removeNonexistentPatternIsNoOp() {
    enforcer.removeDenyPattern(caseId, "never-added", TENANT);

    assertThat(enforcer.dynamicDenyPatterns(caseId, TENANT)).isEmpty();
  }

  private ImprovementRequest makeRequest(List<String> paths) {
    return new ImprovementRequest(
        "operational", "recipe", "target", "casehubio/engine", paths, 10, Map.of());
  }
}
