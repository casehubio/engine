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
package io.casehub.engine.persistence.datastore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import java.time.Instant;
import java.util.UUID;

public final class CaseInstanceMapper {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private CaseInstanceMapper() {}

  public static ObjectNode toRecord(CaseInstance instance) {
    ObjectNode node = MAPPER.createObjectNode();

    node.put("uuid", instance.getUuid().toString());
    node.put("version", instance.getVersion());
    node.put("state", instance.getState().name());

    if (instance.getActorId() != null) {
      node.put("actorId", instance.getActorId());
    }
    if (instance.getCreatedAt() != null) {
      node.put("createdAt", instance.getCreatedAt().toString());
    }

    CaseMetaModel meta = instance.getCaseMetaModel();
    if (meta != null) {
      node.put("namespace", meta.getNamespace());
      node.put("name", meta.getName());
      node.put("metaVersion", meta.getVersion());
      if (meta.getTitle() != null) {
        node.put("metaTitle", meta.getTitle());
      }
    }

    if (instance.getContextSnapshot() != null) {
      node.set("contextSnapshot", instance.getContextSnapshot());
    }

    ArrayNode labelsNode = MAPPER.createArrayNode();
    instance.getLabels().forEach(labelsNode::add);
    node.set("labels", labelsNode);

    ArrayNode typesNode = MAPPER.createArrayNode();
    instance.getTypes().forEach(typesNode::add);
    node.set("types", typesNode);

    if (!instance.getExchangeHeaders().isEmpty()) {
      node.set("exchangeHeaders", MAPPER.valueToTree(instance.getExchangeHeaders()));
    }

    if (instance.getParentCaseId() != null) {
      node.put("parentCaseId", instance.getParentCaseId().toString());
    }
    if (instance.getParentPlanItemId() != null) {
      node.put("parentPlanItemId", instance.getParentPlanItemId().toString());
    }
    if (instance.getWaitingForWorkId() != null) {
      node.put("waitingForWorkId", instance.getWaitingForWorkId());
    }

    return node;
  }

  public static CaseInstance fromRecord(ObjectNode node) {
    CaseInstance instance = new CaseInstance();

    instance.setUuid(UUID.fromString(node.get("uuid").asText()));
    instance.setVersion(node.has("version") ? node.get("version").asLong() : 0);
    instance.setState(CaseStatus.valueOf(node.get("state").asText()));

    if (node.has("actorId") && !node.get("actorId").isNull()) {
      instance.setActorId(node.get("actorId").asText());
    }
    if (node.has("createdAt") && !node.get("createdAt").isNull()) {
      instance.setCreatedAt(Instant.parse(node.get("createdAt").asText()));
    }

    CaseMetaModel meta = new CaseMetaModel();
    if (node.has("namespace")) {
      meta.setNamespace(node.get("namespace").asText());
    }
    if (node.has("name")) {
      meta.setName(node.get("name").asText());
    }
    if (node.has("metaVersion")) {
      meta.setVersion(node.get("metaVersion").asText());
    }
    if (node.has("metaTitle") && !node.get("metaTitle").isNull()) {
      meta.setTitle(node.get("metaTitle").asText());
    }
    instance.setCaseMetaModel(meta);

    if (node.has("contextSnapshot")) {
      instance.setContextSnapshot(node.get("contextSnapshot"));
    }

    if (node.has("labels")) {
      node.get("labels").forEach(n -> instance.getLabels().add(n.asText()));
    }
    if (node.has("types")) {
      node.get("types").forEach(n -> instance.getTypes().add(n.asText()));
    }

    if (node.has("exchangeHeaders")) {
      ObjectNode headers = (ObjectNode) node.get("exchangeHeaders");
      headers
          .fields()
          .forEachRemaining(
              f ->
                  instance
                      .getExchangeHeaders()
                      .put(
                          f.getKey(),
                          f.getValue().isTextual() ? f.getValue().asText() : f.getValue()));
    }

    if (node.has("parentCaseId") && !node.get("parentCaseId").isNull()) {
      instance.setParentCaseId(UUID.fromString(node.get("parentCaseId").asText()));
    }
    if (node.has("parentPlanItemId") && !node.get("parentPlanItemId").isNull()) {
      instance.setParentPlanItemId(UUID.fromString(node.get("parentPlanItemId").asText()));
    }
    if (node.has("waitingForWorkId") && !node.get("waitingForWorkId").isNull()) {
      instance.setWaitingForWorkId(node.get("waitingForWorkId").asText());
    }

    return instance;
  }
}
