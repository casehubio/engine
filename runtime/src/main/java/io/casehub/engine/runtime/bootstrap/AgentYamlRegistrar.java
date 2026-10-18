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

import io.casehub.engine.common.spi.AgentDefinition;
import io.casehub.engine.common.spi.AgentDefinitionRegistry;
import io.casehub.platform.bootstrap.spi.ClasspathYamlRegistrar;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

@ApplicationScoped
public class AgentYamlRegistrar implements ClasspathYamlRegistrar {

  private static final Logger LOG = Logger.getLogger(AgentYamlRegistrar.class);

  @Inject AgentDefinitionRegistry agentDefinitionRegistry;

  @Override
  public String sectionKey() {
    return "agents";
  }

  @Override
  public Set<String> classpathPaths() {
    return Set.of("agents");
  }

  @Override
  public int priority() {
    return 100;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void register(String resourcePath, InputStream content) {
    Yaml yaml = new Yaml();
    Map<String, Object> doc = yaml.load(content);
    if (doc == null) {
      LOG.warnf("Empty agent definition in %s", resourcePath);
      return;
    }

    String name = (String) doc.get("name");
    String type = (String) doc.get("type");
    String description = (String) doc.get("description");
    Map<String, Object> config =
        doc.containsKey("configuration")
            ? (Map<String, Object>) doc.get("configuration")
            : Map.of();

    if (name == null) {
      LOG.warnf("Agent definition in %s missing required 'name' field", resourcePath);
      return;
    }

    AgentDefinition agent = new AgentDefinition(name, type, description, config);
    agentDefinitionRegistry.registerAgent(agent);
    LOG.debugf("Registered agent definition from %s: %s", resourcePath, name);
  }
}
