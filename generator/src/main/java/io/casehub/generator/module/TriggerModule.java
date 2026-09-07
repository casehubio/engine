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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import io.casehub.api.model.CloudEventTrigger;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.ScheduleTrigger;
import io.casehub.api.model.ScopeActivatedTrigger;
import io.casehub.api.model.Trigger;

/**
 * Generates schemas for {@link Trigger} and all its sub-types. The parent Trigger uses a
 * named-property oneOf discriminator; sub-types are referenced via {@code
 * createDefinitionReference} so victools generates their defs. {@link CloudEventTrigger} uses a
 * scalar-or-object shorthand.
 */
public class TriggerModule implements Module {

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    builder
        .forTypesInGeneral()
        .withCustomDefinitionProvider(
            (type, context) -> {
              Class<?> raw = type.getErasedType();
              if (raw == Trigger.class) {
                return new CustomDefinition(buildTrigger(context));
              }
              if (raw == CloudEventTrigger.class) {
                return new CustomDefinition(buildCloudEventTrigger(context));
              }
              if (raw == ScheduleTrigger.class) {
                return new CustomDefinition(buildScheduleTrigger(context));
              }
              if (raw == ContextChangeTrigger.class) {
                return new CustomDefinition(buildContextChangeTrigger(context));
              }
              if (raw == ScopeActivatedTrigger.class) {
                return new CustomDefinition(buildScopeActivatedTrigger(context));
              }
              return null;
            });
  }

  private static ObjectNode buildTrigger(SchemaGenerationContext context) {
    ObjectNode schema = context.getGeneratorConfig().createObjectNode();
    schema.put("type", "object");
    schema.put(
        "description",
        "Defines what the Worker observes. Exactly one of:"
            + " contextChange, cloudEvent, schedule, scopeActivated.");
    schema.put("unevaluatedProperties", false);

    ArrayNode oneOf = schema.putArray("oneOf");
    oneOf.addObject().putArray("required").add("contextChange");
    oneOf.addObject().putArray("required").add("cloudEvent");
    oneOf.addObject().putArray("required").add("schedule");
    oneOf.addObject().putArray("required").add("scopeActivated");

    ObjectNode properties = schema.putObject("properties");
    ResolvedType ccType = context.getTypeContext().resolve(ContextChangeTrigger.class);
    ResolvedType ceType = context.getTypeContext().resolve(CloudEventTrigger.class);
    ResolvedType stType = context.getTypeContext().resolve(ScheduleTrigger.class);
    ResolvedType saType = context.getTypeContext().resolve(ScopeActivatedTrigger.class);
    properties.set("contextChange", context.createDefinitionReference(ccType));
    properties.set("cloudEvent", context.createDefinitionReference(ceType));
    properties.set("schedule", context.createDefinitionReference(stType));
    properties.set("scopeActivated", context.createDefinitionReference(saType));

    return schema;
  }

  private static ObjectNode buildCloudEventTrigger(SchemaGenerationContext context) {
    ObjectNode n = context.getGeneratorConfig().createObjectNode();
    n.put("description", "Fires on matching CloudEvents.");
    ArrayNode oneOf = n.putArray("oneOf");
    oneOf.addObject().put("type", "string").put("description", "CloudEvent type exact match");
    ObjectNode objVariant = oneOf.addObject();
    objVariant.put("type", "object");
    objVariant.putArray("required").add("type");
    objVariant.put("unevaluatedProperties", false);
    objVariant.put("additionalProperties", false);
    ObjectNode props = objVariant.putObject("properties");
    props.putObject("type").put("type", "string");
    props.putObject("source").put("type", "string");
    props.putObject("subject").put("type", "string");
    props.putObject("filter").put("$ref", "#/$defs/ExpressionOrOverride");
    return n;
  }

  private static ObjectNode buildScheduleTrigger(SchemaGenerationContext context) {
    ObjectNode n = context.getGeneratorConfig().createObjectNode();
    n.put("type", "object");
    n.put("description", "Time-based trigger.");
    n.put("unevaluatedProperties", false);
    n.put("additionalProperties", false);
    ArrayNode oneOf = n.putArray("oneOf");
    oneOf.addObject().putArray("required").add("cron");
    oneOf.addObject().putArray("required").add("every");
    ObjectNode props = n.putObject("properties");
    props.putObject("cron").put("type", "string");
    props.putObject("every").put("type", "string");
    props.putObject("timezone").put("type", "string");
    return n;
  }

  private static ObjectNode buildScopeActivatedTrigger(SchemaGenerationContext context) {
    ObjectNode n = context.getGeneratorConfig().createObjectNode();
    n.put("type", "object");
    n.put("unevaluatedProperties", false);
    n.put("additionalProperties", false);
    return n;
  }

  private static ObjectNode buildContextChangeTrigger(SchemaGenerationContext context) {
    ObjectNode n = context.getGeneratorConfig().createObjectNode();
    n.put("type", "object");
    n.put("unevaluatedProperties", false);
    n.put("additionalProperties", false);
    ObjectNode props = n.putObject("properties");
    props.putObject("filter").put("$ref", "#/$defs/ExpressionOrOverride");
    props.putObject("listenLayer").put("type", "string");
    return n;
  }
}
