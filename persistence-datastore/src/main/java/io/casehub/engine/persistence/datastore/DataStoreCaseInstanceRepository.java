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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.model.CaseStatus;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.query.CaseInstanceQuery;
import io.casehub.platform.api.data.DataCollection;
import io.casehub.platform.api.data.DataContext;
import io.casehub.platform.api.data.DataStore;
import io.casehub.platform.api.data.Filter;
import io.casehub.platform.api.data.FilterOp;
import io.casehub.platform.api.data.MergeStrategy;
import io.casehub.platform.api.data.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DataStoreCaseInstanceRepository implements CaseInstanceRepository {

  private static final String APP_ID = "engine";
  private final DataCollection collection;
  private final AtomicLong idSeq = new AtomicLong(1);

  public DataStoreCaseInstanceRepository(DataStore dataStore) {
    dataStore.register(CaseInstanceSchema.descriptor());
    this.collection = dataStore.collection(CaseInstanceSchema.COLLECTION);
  }

  @Override
  public CaseInstance save(CaseInstance instance, String tenancyId) {
    instance.id = idSeq.getAndIncrement();
    instance.tenancyId = tenancyId;
    ObjectNode record = CaseInstanceMapper.toRecord(instance);
    collection.insert(instance.getUuid().toString(), record, ctx(tenancyId));
    return instance;
  }

  @Override
  public CaseInstance update(CaseInstance instance, String tenancyId) {
    ObjectNode record = CaseInstanceMapper.toRecord(instance);
    collection.update(
        instance.getUuid().toString(),
        record,
        OptionalInt.empty(),
        MergeStrategy.REPLACE,
        ctx(tenancyId));
    return instance;
  }

  @Override
  public Optional<CaseInstance> findByUuid(UUID uuid, String tenancyId) {
    return collection
        .get(uuid.toString(), ctx(tenancyId))
        .map(
            node -> {
              CaseInstance ci = CaseInstanceMapper.fromRecord(node);
              ci.tenancyId = tenancyId;
              return ci;
            });
  }

  @Override
  public void updateStateAndAppendEvent(
      CaseInstance instance, EventLog eventLog, String tenancyId) {
    update(instance, tenancyId);
  }

  @Override
  public List<CaseInstance> findByStatus(CaseStatus status, String tenancyId) {
    Query q =
        new Query(
            List.of(new Filter("state", FilterOp.EQ, status.name())),
            List.of(),
            0,
            Integer.MAX_VALUE);
    return toInstances(collection.query(q, ctx(tenancyId)).records(), tenancyId);
  }

  @Override
  public List<CaseInstance> findAll(String tenancyId) {
    Query q = new Query(List.of(), List.of(), 0, Integer.MAX_VALUE);
    return toInstances(collection.query(q, ctx(tenancyId)).records(), tenancyId);
  }

  @Override
  public List<CaseInstance> findByNamespaceAndName(
      String namespace, String name, String tenancyId) {
    Query q =
        new Query(
            List.of(
                new Filter("namespace", FilterOp.EQ, namespace),
                new Filter("name", FilterOp.EQ, name)),
            List.of(),
            0,
            Integer.MAX_VALUE);
    return toInstances(collection.query(q, ctx(tenancyId)).records(), tenancyId);
  }

  @Override
  public List<CaseInstance> query(CaseInstanceQuery query, String tenancyId) {
    List<Filter> filters = new ArrayList<>();
    if (query.namespace() != null) {
      filters.add(new Filter("namespace", FilterOp.EQ, query.namespace()));
    }
    if (query.name() != null) {
      filters.add(new Filter("name", FilterOp.EQ, query.name()));
    }
    if (query.status() != null) {
      filters.add(new Filter("state", FilterOp.EQ, query.status().name()));
    }

    Query q = new Query(filters, List.of(), query.page(), query.size());
    List<CaseInstance> results =
        toInstances(collection.query(q, ctx(tenancyId)).records(), tenancyId);

    if (query.query() != null && !query.query().isEmpty()) {
      String lower = query.query().toLowerCase();
      results =
          results.stream()
              .filter(
                  ci -> {
                    String ns =
                        ci.getCaseMetaModel() != null ? ci.getCaseMetaModel().getNamespace() : "";
                    String nm =
                        ci.getCaseMetaModel() != null ? ci.getCaseMetaModel().getName() : "";
                    return (ns != null && ns.toLowerCase().contains(lower))
                        || (nm != null && nm.toLowerCase().contains(lower));
                  })
              .toList();
    }

    return results;
  }

  @Override
  public long count(CaseInstanceQuery query, String tenancyId) {
    List<Filter> filters = new ArrayList<>();
    if (query.namespace() != null) {
      filters.add(new Filter("namespace", FilterOp.EQ, query.namespace()));
    }
    if (query.name() != null) {
      filters.add(new Filter("name", FilterOp.EQ, query.name()));
    }
    if (query.status() != null) {
      filters.add(new Filter("state", FilterOp.EQ, query.status().name()));
    }

    Query q = new Query(filters, List.of(), 0, 1);
    return collection.count(q, ctx(tenancyId));
  }

  private DataContext ctx(String tenancyId) {
    return new DataContext(tenancyId, APP_ID);
  }

  private List<CaseInstance> toInstances(List<ObjectNode> records, String tenancyId) {
    return records.stream()
        .map(
            node -> {
              CaseInstance ci = CaseInstanceMapper.fromRecord(node);
              ci.tenancyId = tenancyId;
              return ci;
            })
        .toList();
  }
}
