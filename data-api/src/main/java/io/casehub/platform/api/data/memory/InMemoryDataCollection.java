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
package io.casehub.platform.api.data.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.platform.api.data.DataCollection;
import io.casehub.platform.api.data.DataContext;
import io.casehub.platform.api.data.Filter;
import io.casehub.platform.api.data.FilterOp;
import io.casehub.platform.api.data.MergeStrategy;
import io.casehub.platform.api.data.Query;
import io.casehub.platform.api.data.QueryResult;
import io.casehub.platform.api.data.RecordNotFoundException;
import io.casehub.platform.api.data.SortDirection;
import io.casehub.platform.api.data.SortField;
import io.casehub.platform.api.data.VersionConflictException;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryDataCollection implements DataCollection {

  private final Map<String, ObjectNode> store = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> versions = new ConcurrentHashMap<>();

  private String key(String id, DataContext ctx) {
    return ctx.tenantId() + "::" + ctx.appId() + "::" + id;
  }

  @Override
  public String insert(ObjectNode record, DataContext ctx) {
    return insert(UUID.randomUUID().toString(), record, ctx);
  }

  @Override
  public String insert(String id, ObjectNode record, DataContext ctx) {
    ObjectNode clean = stripSystemFields(record);
    String key = key(id, ctx);
    String now = Instant.now().toString();

    clean.put("_id", id);
    clean.put("_version", 1);
    clean.put("_created_at", now);
    clean.put("_updated_at", now);

    store.put(key, clean);
    versions.put(key, new AtomicInteger(1));
    return id;
  }

  @Override
  public Optional<ObjectNode> get(String id, DataContext ctx) {
    ObjectNode node = store.get(key(id, ctx));
    return Optional.ofNullable(node).map(this::defensiveCopy);
  }

  @Override
  public void update(
      String id,
      ObjectNode changes,
      OptionalInt expectedVersion,
      MergeStrategy mergeStrategy,
      DataContext ctx) {
    String key = key(id, ctx);
    ObjectNode existing = store.get(key);
    if (existing == null) {
      throw new RecordNotFoundException("collection", id);
    }

    AtomicInteger ver = versions.get(key);
    if (expectedVersion.isPresent() && ver.get() != expectedVersion.getAsInt()) {
      throw new VersionConflictException("collection", id, expectedVersion.getAsInt(), ver.get());
    }

    ObjectNode merged = applyMerge(existing, stripSystemFields(changes), mergeStrategy);
    int newVersion = ver.incrementAndGet();
    merged.put("_id", id);
    merged.put("_version", newVersion);
    merged.put("_created_at", existing.get("_created_at").asText());
    merged.put("_updated_at", Instant.now().toString());

    store.put(key, merged);
  }

  @Override
  public void delete(String id, DataContext ctx) {
    String key = key(id, ctx);
    if (store.remove(key) == null) {
      throw new RecordNotFoundException("collection", id);
    }
    versions.remove(key);
  }

  @Override
  public QueryResult query(Query query, DataContext ctx) {
    List<ObjectNode> all = allForContext(ctx);
    Stream<ObjectNode> stream = all.stream();

    for (Filter filter : query.filters()) {
      stream = stream.filter(node -> matchesFilter(node, filter));
    }

    List<ObjectNode> filtered = stream.collect(Collectors.toList());

    if (!query.sort().isEmpty()) {
      filtered.sort(
          (a, b) -> {
            for (SortField sf : query.sort()) {
              String va = a.has(sf.field()) ? a.get(sf.field()).asText() : "";
              String vb = b.has(sf.field()) ? b.get(sf.field()).asText() : "";
              int cmp = va.compareTo(vb);
              if (sf.direction() == SortDirection.DESC) {
                cmp = -cmp;
              }
              if (cmp != 0) {
                return cmp;
              }
            }
            return 0;
          });
    }

    long total = filtered.size();
    int skip = query.page() * query.size();
    List<ObjectNode> page =
        filtered.stream().skip(skip).limit(query.size()).map(this::defensiveCopy).toList();

    return new QueryResult(page, total, query.page(), query.size());
  }

  @Override
  public long count(Query query, DataContext ctx) {
    Stream<ObjectNode> stream = allForContext(ctx).stream();
    for (Filter filter : query.filters()) {
      stream = stream.filter(node -> matchesFilter(node, filter));
    }
    return stream.count();
  }

  @Override
  public List<ObjectNode> search(String text, DataContext ctx) {
    String lower = text.toLowerCase();
    return allForContext(ctx).stream()
        .filter(
            node -> {
              Iterator<JsonNode> it = node.elements();
              while (it.hasNext()) {
                JsonNode val = it.next();
                if (val.isTextual() && val.asText().toLowerCase().contains(lower)) {
                  return true;
                }
              }
              return false;
            })
        .map(this::defensiveCopy)
        .toList();
  }

  void clearAll() {
    store.clear();
    versions.clear();
  }

  private List<ObjectNode> allForContext(DataContext ctx) {
    String prefix = ctx.tenantId() + "::" + ctx.appId() + "::";
    return store.entrySet().stream()
        .filter(e -> e.getKey().startsWith(prefix))
        .map(Map.Entry::getValue)
        .toList();
  }

  private boolean matchesFilter(ObjectNode node, Filter filter) {
    JsonNode fieldValue = node.get(filter.field());
    if (filter.op() == FilterOp.EXISTS) {
      return fieldValue != null && !fieldValue.isNull();
    }
    if (fieldValue == null || fieldValue.isNull()) {
      return false;
    }

    String actual = fieldValue.asText();
    return switch (filter.op()) {
      case EQ -> actual.equals(filter.value().toString());
      case NEQ -> !actual.equals(filter.value().toString());
      case CONTAINS -> actual.contains(filter.value().toString());
      case IN -> {
        if (filter.value() instanceof Collection<?> c) {
          yield c.stream().anyMatch(v -> actual.equals(v.toString()));
        }
        yield false;
      }
      default -> true;
    };
  }

  private ObjectNode stripSystemFields(ObjectNode node) {
    ObjectNode copy = node.deepCopy();
    copy.remove("_id");
    copy.remove("_version");
    copy.remove("_created_at");
    copy.remove("_updated_at");
    return copy;
  }

  private ObjectNode defensiveCopy(ObjectNode node) {
    return node.deepCopy();
  }

  private ObjectNode applyMerge(ObjectNode existing, ObjectNode changes, MergeStrategy strategy) {
    return switch (strategy) {
      case REPLACE -> changes.deepCopy();
      case SHALLOW -> {
        ObjectNode merged = stripSystemFields(existing).deepCopy();
        changes.fields().forEachRemaining(f -> merged.set(f.getKey(), f.getValue()));
        yield merged;
      }
      case DEEP, RFC_7396 -> {
        ObjectNode merged = stripSystemFields(existing).deepCopy();
        deepMerge(merged, changes);
        yield merged;
      }
    };
  }

  private void deepMerge(ObjectNode target, ObjectNode source) {
    source
        .fields()
        .forEachRemaining(
            f -> {
              if (f.getValue().isNull()) {
                target.remove(f.getKey());
              } else if (f.getValue().isObject()
                  && target.has(f.getKey())
                  && target.get(f.getKey()).isObject()) {
                deepMerge((ObjectNode) target.get(f.getKey()), (ObjectNode) f.getValue());
              } else {
                target.set(f.getKey(), f.getValue());
              }
            });
  }
}
