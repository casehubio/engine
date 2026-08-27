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
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.model.cbr.CbrConfig.CbrRetrievalTiming;
import java.io.IOException;

public class CbrConfigDeserializer extends StdDeserializer<CbrConfig> {

  public CbrConfigDeserializer() {
    super(CbrConfig.class);
  }

  @Override
  public CbrConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.readValueAsTree();
    if (node == null || node.isNull()) {
      return null;
    }

    CbrConfig.Builder builder = CbrConfig.builder();

    String featuresKey =
        node.has("features")
            ? "features"
            : node.has("featureExtractor") ? "featureExtractor" : null;
    if (featuresKey != null && node.get(featuresKey).isObject()) {
      node.get(featuresKey)
          .fields()
          .forEachRemaining(e -> builder.feature(e.getKey(), e.getValue().asText()));
    }

    if (node.has("topK")) builder.topK(node.get("topK").asInt());
    if (node.has("minSimilarity")) builder.minSimilarity(node.get("minSimilarity").asDouble());
    if (node.has("domain")) builder.domain(node.get("domain").asText());
    if (node.has("caseType")) builder.caseType(node.get("caseType").asText());
    if (node.has("cbrType")) builder.cbrType(node.get("cbrType").asText());
    if (node.has("vectorWeight")) builder.vectorWeight(node.get("vectorWeight").asDouble());
    if (node.has("temporalDecayHalfLifeDays"))
      builder.temporalDecayHalfLifeDays(node.get("temporalDecayHalfLifeDays").asInt());
    if (node.has("minCostSamples")) builder.minCostSamples(node.get("minCostSamples").asInt());
    if (node.has("timing")) {
      builder.timing(
          CbrRetrievalTiming.valueOf(node.get("timing").asText().toUpperCase().replace("-", "_")));
    }
    if (node.has("weights") && node.get("weights").isObject()) {
      node.get("weights")
          .fields()
          .forEachRemaining(e -> builder.weight(e.getKey(), e.getValue().asDouble()));
    }

    return builder.build();
  }

  @Override
  public CbrConfig getNullValue(DeserializationContext ctxt) {
    return null;
  }
}
