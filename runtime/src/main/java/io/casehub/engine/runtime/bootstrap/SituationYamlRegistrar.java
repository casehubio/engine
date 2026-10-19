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

  private static final com.fasterxml.jackson.databind.ObjectMapper yamlMapper = createYamlMapper();

  @jakarta.inject.Inject
  jakarta.enterprise.inject.Instance<io.casehub.ras.api.SituationRegistrar> situationRegistrar;

  private static com.fasterxml.jackson.databind.ObjectMapper createYamlMapper() {
    var mapper =
        new com.fasterxml.jackson.databind.ObjectMapper(
            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    var module = new com.fasterxml.jackson.databind.module.SimpleModule();
    module.addDeserializer(
        io.casehub.platform.api.expression.ExpressionEvaluator.class,
        new io.casehub.api.model.converter.deser.ExpressionEvaluatorDeserializer(null));
    mapper.registerModule(module);
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

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
    try {
      io.casehub.ras.api.SituationDefinition definition =
          yamlMapper.readValue(content, io.casehub.ras.api.SituationDefinition.class);
      if (!situationRegistrar.isResolvable()) {
        LOG.infof(
            "Discovered situation definition '%s' at %s (SituationRegistrar not available — registration deferred)",
            definition.situationId(), resourcePath);
        return;
      }
      situationRegistrar.get().register(new io.casehub.ras.api.SituationRegistration(definition));
      LOG.debugf(
          "Registered situation definition from %s: %s", resourcePath, definition.situationId());
    } catch (java.io.IOException e) {
      LOG.errorf(e, "Failed to load situation definition from %s", resourcePath);
    } catch (Exception e) {
      LOG.errorf(e, "Failed to register situation definition from %s", resourcePath);
    }
  }
}
