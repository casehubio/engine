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
package io.casehub.engine.annotations.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ExecutionMode;
import io.casehub.api.model.LifecycleScope;
import io.casehub.api.model.Participation;
import io.casehub.engine.annotations.Bind;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Worker;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class BindingAttributeWiringTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest().withApplicationRoot(root -> root.addClasses(AttributeCase.class));

  @Case(namespace = "test", name = "AttributeWiring", version = "1.0.0")
  public interface AttributeCase {

    @Worker(
        scope = LifecycleScope.COMPOUND,
        participation = Participation.COMPANION,
        executionMode = ExecutionMode.PERSISTENT)
    @Bind(
        contextChange = ".ready",
        conflictStrategy = "DEEP_MERGE",
        producedKeys = {"result", "status"})
    default void monitor() {}
  }

  @Inject CaseDefinition definition;

  @Test
  void lifecycle_scope_is_wired() {
    var binding =
        definition.getBindings().stream()
            .filter(b -> b.getName().equals("monitor"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.lifecycleScope()).isEqualTo(LifecycleScope.COMPOUND);
  }

  @Test
  void participation_is_wired() {
    var binding =
        definition.getBindings().stream()
            .filter(b -> b.getName().equals("monitor"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.participation()).isEqualTo(Participation.COMPANION);
  }

  @Test
  void execution_mode_is_wired() {
    var binding =
        definition.getBindings().stream()
            .filter(b -> b.getName().equals("monitor"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.executionMode()).isEqualTo(ExecutionMode.PERSISTENT);
  }

  @Test
  void conflict_strategy_is_wired() {
    var binding =
        definition.getBindings().stream()
            .filter(b -> b.getName().equals("monitor"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.getConflictResolverStrategy()).isEqualTo("DEEP_MERGE");
  }

  @Test
  void produced_keys_are_wired() {
    var binding =
        definition.getBindings().stream()
            .filter(b -> b.getName().equals("monitor"))
            .findFirst()
            .orElseThrow();
    assertThat(binding.getProducedKeys()).isEqualTo(Set.of("result", "status"));
  }
}
