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
import io.casehub.engine.annotations.Bind;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Customize;
import io.casehub.engine.annotations.Worker;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CustomizeMultiParamTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(
              root ->
                  root.addClasses(MultiParamCase.class, TitleProvider.class)
                      .addAsManifestResource(
                          new org.jboss.shrinkwrap.api.asset.StringAsset(""), "beans.xml"));

  @ApplicationScoped
  @io.quarkus.arc.Unremovable
  public static class TitleProvider {
    public String getTitle() {
      return "Injected Title";
    }
  }

  @Case(namespace = "test", name = "MultiParam", version = "1.0.0")
  public interface MultiParamCase {

    @Worker(capability = "work")
    @Bind(contextChange = ".input != null")
    default String work(String input) {
      return "done";
    }

    @Customize
    static void customize(CaseDefinition.Builder builder, TitleProvider provider) {
      builder.title(provider.getTitle());
    }
  }

  @Inject CaseDefinition definition;

  @Test
  void customizer_with_cdi_injection() {
    assertThat(definition.getTitle()).isEqualTo("Injected Title");
  }
}
