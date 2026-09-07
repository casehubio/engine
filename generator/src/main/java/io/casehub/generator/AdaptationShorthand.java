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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import io.casehub.schema.generator.module.ShorthandDefinition;

final class AdaptationShorthand {

  private AdaptationShorthand() {}

  static ShorthandDefinition definition() {
    return ShorthandDefinition.of(
        AdaptationShorthand::scalarSchema, AdaptationShorthand::objectSchema);
  }

  private static ObjectNode scalarSchema(SchemaGeneratorConfig config) {
    ObjectNode n = config.createObjectNode();
    n.put("type", "string");
    n.putArray("enum").add("adaptive").add("conservative").add("off").add("progress");
    return n;
  }

  private static ObjectNode objectSchema(SchemaGeneratorConfig config) {
    ObjectNode n = config.createObjectNode();
    n.put("type", "object");
    n.put("unevaluatedProperties", false);
    ObjectNode props = n.putObject("properties");
    props.putObject("trigger").put("type", "string");
    props.putObject("optimization").put("type", "string");
    props.putObject("revision").put("type", "string");
    ObjectNode threshold = props.putObject("threshold");
    threshold.put("type", "number");
    threshold.put("minimum", 0);
    threshold.put("maximum", 1);
    props.putObject("metaReasoner").put("type", "string");
    props.putObject("repair").put("type", "string");
    ObjectNode contThreshold = props.putObject("contingencyThreshold");
    contThreshold.put("type", "number");
    contThreshold.put("minimum", 0);
    contThreshold.put("maximum", 1);
    return n;
  }
}
