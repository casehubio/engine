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
package io.casehub.engine.persistence.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.pages.data.DataContext;
import io.casehub.pages.data.json.JsonCollectionStore;
import io.casehub.pages.data.json.MergeStrategy;
import io.casehub.platform.api.identity.CurrentPrincipal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Single-user, low-concurrency cache for YAML-only helpdesk apps. Does not maintain
 * shared-reference identity — each {@code get()} deserializes a fresh object — so it must not be
 * deployed alongside modules that rely on in-process cache mutation or {@code
 * CaseInstance.stateLock} concurrency guards.
 */
public class JsonCollectionCaseInstanceCache implements CaseInstanceCache {

  private static final Logger LOG = Logger.getLogger(JsonCollectionCaseInstanceCache.class);
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final String APP_ID = "casehub-engine";
  private static final String COLLECTION = "case-instances";

  private final JsonCollectionStore store;
  private final CaseDefinitionRegistry registry;
  private final CurrentPrincipal principal;

  public JsonCollectionCaseInstanceCache(
      JsonCollectionStore store, CaseDefinitionRegistry registry, CurrentPrincipal principal) {
    this.store = store;
    this.registry = registry;
    this.principal = principal;
  }

  @Override
  public void put(CaseInstance instance) {
    CaseInstanceDocument doc = toDocument(instance);
    JsonNode json = MAPPER.valueToTree(doc);
    DataContext ctx = new DataContext(principal.tenancyId());
    String docId = instance.getUuid().toString();

    Optional<JsonNode> existing = store.get(ctx, APP_ID, COLLECTION, docId);
    if (existing.isPresent()) {
      store.update(
          ctx, APP_ID, COLLECTION, docId, json, OptionalInt.empty(), MergeStrategy.SHALLOW);
    } else {
      store.create(ctx, APP_ID, COLLECTION, json, "uuid");
    }
  }

  @Override
  public CaseInstance get(UUID caseId) {
    DataContext ctx = new DataContext(principal.tenancyId());
    Optional<JsonNode> doc = store.get(ctx, APP_ID, COLLECTION, caseId.toString());
    return doc.map(this::fromDocument).orElse(null);
  }

  @Override
  public void clear() {
    DataContext ctx = new DataContext(principal.tenancyId());
    List<JsonNode> all = store.listAll(principal.tenancyId(), APP_ID, COLLECTION);
    for (JsonNode doc : all) {
      JsonNode uuidNode = doc.get("uuid");
      if (uuidNode != null) {
        store.delete(ctx, APP_ID, COLLECTION, uuidNode.asText());
      }
    }
  }

  @Override
  public List<CaseInstance> getAll() {
    List<JsonNode> docs = store.listAll(principal.tenancyId(), APP_ID, COLLECTION);
    return docs.stream().map(this::fromDocument).filter(Objects::nonNull).toList();
  }

  private CaseInstanceDocument toDocument(CaseInstance instance) {
    CaseMetaModel meta = instance.getCaseMetaModel();
    return new CaseInstanceDocument(
        instance.getUuid().toString(),
        instance.getState() != null ? instance.getState().name() : null,
        instance.getVersion(),
        meta != null ? meta.getNamespace() : null,
        meta != null ? meta.getName() : null,
        meta != null ? meta.getVersion() : null,
        instance.getContextSnapshot(),
        instance.getParentCaseId() != null ? instance.getParentCaseId().toString() : null,
        instance.getParentPlanItemId() != null ? instance.getParentPlanItemId().toString() : null,
        instance.getActorId(),
        instance.getLabels(),
        instance.getTypes(),
        instance.getCreatedAt() != null ? instance.getCreatedAt().toString() : null);
  }

  private CaseInstance fromDocument(JsonNode json) {
    try {
      CaseInstanceDocument doc = MAPPER.treeToValue(json, CaseInstanceDocument.class);
      CaseInstance instance = new CaseInstance();
      instance.setUuid(UUID.fromString(doc.uuid()));
      if (doc.state() != null) {
        instance.setState(CaseStatus.valueOf(doc.state()));
      }
      instance.setVersion(doc.version());
      instance.setActorId(doc.actorId());
      instance.setContextSnapshot(doc.contextSnapshot());
      instance.setLabels(doc.labels() != null ? new HashSet<>(doc.labels()) : new HashSet<>());
      instance.setTypes(doc.types() != null ? new HashSet<>(doc.types()) : new HashSet<>());
      if (doc.parentCaseId() != null) {
        instance.setParentCaseId(UUID.fromString(doc.parentCaseId()));
      }
      if (doc.parentPlanItemId() != null) {
        instance.setParentPlanItemId(UUID.fromString(doc.parentPlanItemId()));
      }
      if (doc.createdAt() != null) {
        instance.setCreatedAt(Instant.parse(doc.createdAt()));
      }

      if (doc.metaNamespace() != null && doc.metaName() != null && doc.metaVersion() != null) {
        Optional<CaseMetaModel> meta =
            registry.findByIdentity(doc.metaNamespace(), doc.metaName(), doc.metaVersion());
        meta.ifPresent(instance::setCaseMetaModel);
        if (meta.isEmpty()) {
          CaseMetaModel stub = new CaseMetaModel();
          stub.setNamespace(doc.metaNamespace());
          stub.setName(doc.metaName());
          stub.setVersion(doc.metaVersion());
          instance.setCaseMetaModel(stub);
        }
      }
      return instance;
    } catch (Exception e) {
      LOG.errorf(e, "Failed to deserialize CaseInstance from JSON collection");
      return null;
    }
  }
}
