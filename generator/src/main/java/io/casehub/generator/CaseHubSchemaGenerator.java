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
package io.casehub.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import io.casehub.api.model.AdaptationConfig;
import io.casehub.schema.generator.module.SealedHierarchyModule;
import io.casehub.schema.generator.module.ShorthandModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

public class CaseHubSchemaGenerator {

  private final SchemaGenerator schemaGenerator;

  public CaseHubSchemaGenerator() {
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);

    configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    configBuilder.with(Option.FLATTENED_ENUMS_FROM_TOSTRING);

    configBuilder.with(
        new JakartaValidationModule(
            JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS,
            JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED));
    configBuilder.with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_ORDER));

    configBuilder.with(new io.casehub.schema.generator.module.EnumInliningModule());
    configBuilder.with(new io.casehub.generator.module.WorkerSchemaModule());
    configBuilder.with(new io.casehub.generator.module.CaseCompletionSchemaModule());
    configBuilder.with(new io.casehub.generator.module.ExpressionEvaluatorModule());
    configBuilder.with(new io.casehub.generator.module.TriggerModule());
    configBuilder.with(new io.casehub.generator.module.BindingTargetModule());
    configBuilder.with(new io.casehub.generator.module.SpecNestingModule());
    configBuilder.with(new io.casehub.generator.module.UnevaluatedPropertiesModule());

    configBuilder.with(
        new ShorthandModule(Map.of(AdaptationConfig.class, AdaptationShorthand.definition())));

    // Registered last — existing modules (TriggerModule, BindingTargetModule, etc.)
    // handle their types via withCustomDefinitionProvider which fires first.
    // SealedHierarchyModule auto-generates oneOf for any remaining sealed interfaces.
    configBuilder.with(new SealedHierarchyModule());

    this.schemaGenerator = new SchemaGenerator(configBuilder.build());
  }

  public JsonNode generate(Class<?> rootType) {
    var generated = schemaGenerator.generateSchema(rootType);
    SchemaPostProcessor.process((ObjectNode) generated);
    try {
      return new ObjectMapper().readTree(generated.toString());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to convert schema", e);
    }
  }

  public void generateToYaml(Class<?> rootType, Path output) throws IOException {
    var generated = schemaGenerator.generateSchema(rootType);
    SchemaPostProcessor.process((ObjectNode) generated);
    YAMLMapper yamlMapper =
        YAMLMapper.builder()
            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
            .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
            .build();
    Files.createDirectories(output.getParent());
    yamlMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), generated);
  }
}
