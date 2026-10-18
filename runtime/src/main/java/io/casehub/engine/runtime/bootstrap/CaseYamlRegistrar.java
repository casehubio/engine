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
package io.casehub.engine.runtime.bootstrap;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.converter.CaseDefinitionYamlMapper;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.platform.bootstrap.spi.ClasspathYamlRegistrar;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CaseYamlRegistrar implements ClasspathYamlRegistrar {

  private static final Logger LOG = Logger.getLogger(CaseYamlRegistrar.class);

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  @Override
  public String sectionKey() {
    return "cases";
  }

  @Override
  public Set<String> classpathPaths() {
    return Set.of("casehub", "cases");
  }

  @Override
  public int priority() {
    return 200;
  }

  @Override
  public void register(String resourcePath, InputStream content) {
    try {
      CaseDefinition definition = CaseDefinitionYamlMapper.load(content);
      caseDefinitionRegistry.registerCaseDefinition(definition);
      LOG.debugf("Registered case definition from %s: %s", resourcePath, definition.getName());
    } catch (IOException e) {
      LOG.errorf(e, "Failed to load case definition from %s", resourcePath);
    }
  }
}
