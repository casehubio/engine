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
package io.casehub.api.model.converter.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.casehub.api.model.AdaptationConfig;
import java.io.IOException;

public class AdaptationConfigDeserializer extends StdDeserializer<AdaptationConfig> {

  public AdaptationConfigDeserializer() {
    super(AdaptationConfig.class);
  }

  @Override
  public AdaptationConfig deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonNode node = p.readValueAsTree();

    if (node.isTextual()) {
      return switch (node.asText()) {
        case "adaptive" -> AdaptationConfig.of("every-step", "forward-replan");
        case "conservative" -> AdaptationConfig.of("on-failure", "forward-replan");
        case "progress" ->
            new AdaptationConfig(
                "progress",
                "forward-replan",
                AdaptationConfig.DEFAULT_PROGRESS_THRESHOLD,
                null,
                null,
                null);
        case "off" -> null;
        default ->
            throw ctxt.weirdStringException(
                node.asText(),
                AdaptationConfig.class,
                "unknown preset; expected one of: adaptive, conservative, progress, off");
      };
    }

    if (node.isObject()) {
      String trigger = node.has("trigger") ? node.get("trigger").asText() : "every-step";
      String optimization =
          node.has("optimization")
              ? node.get("optimization").asText()
              : node.has("revision") ? node.get("revision").asText() : "forward-replan";
      Double threshold = node.has("threshold") ? node.get("threshold").asDouble() : null;
      String metaReasoner = node.has("metaReasoner") ? node.get("metaReasoner").asText() : null;
      String repair = node.has("repair") ? node.get("repair").asText() : null;
      Double contingencyThreshold =
          node.has("contingencyThreshold") ? node.get("contingencyThreshold").asDouble() : null;
      return new AdaptationConfig(
          trigger, optimization, threshold, metaReasoner, repair, contingencyThreshold);
    }

    throw ctxt.weirdStringException(
        node.toString(), AdaptationConfig.class, "expected a string preset or an object");
  }

  @Override
  public AdaptationConfig getNullValue(DeserializationContext ctxt) {
    return null;
  }
}
