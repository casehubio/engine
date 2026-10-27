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

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.LifecycleScope;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.engine.annotations.Bind;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Compound;
import io.casehub.engine.annotations.Requires;
import io.casehub.engine.annotations.Worker;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class RequiresAnnotationTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(
              root ->
                  root.addClasses(QuorumCase.class, ExtractResult.class, AggregateResult.class));

  @Compound(
      name = "pipeline",
      workers = {"extract-a", "extract-b", "aggregate"})
  @Case(namespace = "test", name = "QuorumTest", version = "1.0.0")
  public interface QuorumCase {

    @Worker(capability = "extract", scope = LifecycleScope.COMPOUND)
    @Bind(name = "extract-a", scopeActivated = true)
    default ExtractResult extractA() {
      return new ExtractResult("a");
    }

    @Worker(capability = "extract", scope = LifecycleScope.COMPOUND)
    @Bind(name = "extract-b", scopeActivated = true)
    default ExtractResult extractB() {
      return new ExtractResult("b");
    }

    @Worker(capability = "aggregate", scope = LifecycleScope.COMPOUND)
    @Bind(
        name = "aggregate",
        scopeActivated = true,
        requires =
            @Requires(
                bindings = {"extract-a", "extract-b"},
                atLeast = 1))
    default AggregateResult aggregate() {
      return new AggregateResult("done");
    }
  }

  public record ExtractResult(String value) {}

  public record AggregateResult(String value) {}

  @Inject CaseDefinition quorumDefinition;

  @Test
  void binding_has_requires() {
    Binding agg =
        quorumDefinition.getBindings().stream()
            .filter(b -> "aggregate".equals(b.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(agg.getRequires()).isNotNull();
    assertThat(agg.getRequires().bindings()).containsExactly("extract-a", "extract-b");
    assertThat(agg.getRequires().atLeast()).isEqualTo(1);
    assertThat(agg.getRequires().onThresholdReached()).isEqualTo(OnThresholdReached.KEEP);
  }

  @Test
  void binding_without_requires_has_null() {
    Binding extractA =
        quorumDefinition.getBindings().stream()
            .filter(b -> "extract-a".equals(b.getName()))
            .findFirst()
            .orElseThrow();

    assertThat(extractA.getRequires()).isNull();
  }

  @Test
  void binding_name_from_annotation() {
    assertThat(quorumDefinition.getBindings())
        .extracting(Binding::getName)
        .contains("extract-a", "extract-b", "aggregate");
  }
}
