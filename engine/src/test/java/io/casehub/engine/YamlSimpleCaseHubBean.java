package io.casehub.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.api.CaseHub;
import io.casehub.model.CaseHubDefinition;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class YamlSimpleCaseHubBean extends CaseHub {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Override
  public CaseHubDefinition getDefinition() {
    try (InputStream is = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("casehub/simple.yaml")) {
      if (is == null) {
        throw new IllegalStateException("Resource casehub/simple.yaml not found on classpath");
      }
      return YAML_MAPPER.readValue(is, CaseHubDefinition.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load CaseHub definition from casehub/simple.yaml", e);
    }
  }
}
