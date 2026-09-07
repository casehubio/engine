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
package io.casehub.generator.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.casehub.schema.generator.module.SealedHierarchyModule;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SealedHierarchyModuleTest {

  sealed interface Shape permits Circle, Square {}

  record Circle(double radius) implements Shape {}

  record Square(double side) implements Shape {}

  record Container(Shape shape) {}

  @Test
  void sealedInterface_generatesOneOfWithTypeDiscriminator() {
    var configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    configBuilder.with(new SealedHierarchyModule());
    var generator = new SchemaGenerator(configBuilder.build());

    ObjectNode schema = (ObjectNode) generator.generateSchema(Container.class);
    ObjectNode defs = (ObjectNode) schema.get("$defs");

    assertNotNull(defs, "$defs should exist");
    JsonNode shapeDef = defs.get("Shape");
    assertNotNull(shapeDef, "Shape $def should be generated for sealed interface");

    JsonNode oneOf = shapeDef.get("oneOf");
    assertNotNull(oneOf, "Sealed interface should have oneOf");
    assertTrue(oneOf.isArray(), "oneOf should be an array");
    assertEquals(2, oneOf.size(), "Two permits (Circle, Square) = two oneOf entries");

    for (JsonNode variant : oneOf) {
      assertTrue(variant.has("properties"), "Each variant should have properties");
      JsonNode typeField = variant.path("properties").path("type");
      assertTrue(typeField.has("const"), "Each variant should have a type discriminator const");
    }
  }

  @Test
  void discriminatorOverride_usesCustomValues() {
    var configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    configBuilder.with(
        new SealedHierarchyModule(Map.of(Shape.class, Map.of(Circle.class, "round"))));
    var generator = new SchemaGenerator(configBuilder.build());

    ObjectNode schema = (ObjectNode) generator.generateSchema(Container.class);
    JsonNode shapeDef = schema.path("$defs").path("Shape");
    JsonNode oneOf = shapeDef.get("oneOf");

    boolean foundRound = false;
    for (JsonNode variant : oneOf) {
      String discriminator = variant.path("properties").path("type").path("const").asText();
      if ("round".equals(discriminator)) {
        foundRound = true;
      }
    }
    assertTrue(foundRound, "Custom discriminator override 'round' should be used for Circle");
  }

  @Test
  void nonSealedInterface_notAffected() {
    interface OpenInterface {}

    record Holder(String name) {}

    var configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    configBuilder.with(new SealedHierarchyModule());
    var generator = new SchemaGenerator(configBuilder.build());

    ObjectNode schema = (ObjectNode) generator.generateSchema(Holder.class);
    assertTrue(schema.has("properties"), "Non-sealed record should have normal properties");
  }
}
