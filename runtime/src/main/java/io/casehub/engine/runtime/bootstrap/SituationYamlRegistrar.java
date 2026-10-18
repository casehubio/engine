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

import io.casehub.platform.bootstrap.spi.ClasspathYamlRegistrar;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SituationYamlRegistrar implements ClasspathYamlRegistrar {

  private static final Logger LOG = Logger.getLogger(SituationYamlRegistrar.class);

  @Override
  public String sectionKey() {
    return "situations";
  }

  @Override
  public Set<String> classpathPaths() {
    return Set.of("situations");
  }

  @Override
  public int priority() {
    return 300;
  }

  @Override
  public void register(String resourcePath, InputStream content) {
    // TODO: wire to ras-api SituationDefinitionProvider when casehub-ras-api
    //  is added as a dependency — parses YAML into SituationDefinition records
    LOG.infof(
        "Discovered situation definition at %s (registration deferred — ras-api integration pending)",
        resourcePath);
  }
}
