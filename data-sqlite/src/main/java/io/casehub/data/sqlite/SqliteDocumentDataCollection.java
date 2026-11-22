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
package io.casehub.data.sqlite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.platform.api.data.DataCollection;
import io.casehub.platform.api.data.DataContext;
import io.casehub.platform.api.data.DataStoreException;
import io.casehub.platform.api.data.Filter;
import io.casehub.platform.api.data.FilterOp;
import io.casehub.platform.api.data.MergeStrategy;
import io.casehub.platform.api.data.Query;
import io.casehub.platform.api.data.QueryResult;
import io.casehub.platform.api.data.RecordNotFoundException;
import io.casehub.platform.api.data.SortDirection;
import io.casehub.platform.api.data.SortField;
import io.casehub.platform.api.data.VersionConflictException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

public class SqliteDocumentDataCollection implements DataCollection {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final DataSource dataSource;
  private final String collectionName;

  SqliteDocumentDataCollection(DataSource dataSource, String collectionName) {
    this.dataSource = dataSource;
    this.collectionName = collectionName;
  }

  protected DataSource getDataSource() {
    return dataSource;
  }

  protected String getCollectionName() {
    return collectionName;
  }

  @Override
  public String insert(ObjectNode record, DataContext ctx) {
    return insert(UUID.randomUUID().toString(), record, ctx);
  }

  @Override
  public String insert(String id, ObjectNode record, DataContext ctx) {
    ObjectNode clean = stripSystemFields(record);
    String now = Instant.now().toString();

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "INSERT INTO data_records (tenant_id, app_id, collection, doc_id, data, _version,"
                    + " created_at, updated_at) VALUES (?, ?, ?, ?, ?, 1, ?, ?)")) {
      ps.setString(1, ctx.tenantId());
      ps.setString(2, ctx.appId());
      ps.setString(3, collectionName);
      ps.setString(4, id);
      ps.setString(5, MAPPER.writeValueAsString(clean));
      ps.setString(6, now);
      ps.setString(7, now);
      ps.executeUpdate();
    } catch (SQLException | JsonProcessingException e) {
      throw new DataStoreException("Insert failed", e);
    }
    return id;
  }

  @Override
  public Optional<ObjectNode> get(String id, DataContext ctx) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "SELECT data, doc_id, _version, created_at, updated_at FROM data_records"
                    + " WHERE tenant_id = ? AND app_id = ? AND collection = ? AND doc_id = ?")) {
      ps.setString(1, ctx.tenantId());
      ps.setString(2, ctx.appId());
      ps.setString(3, collectionName);
      ps.setString(4, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(toObjectNode(rs));
        }
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new DataStoreException("Get failed", e);
    }
    return Optional.empty();
  }

  @Override
  public void update(
      String id,
      ObjectNode changes,
      OptionalInt expectedVersion,
      MergeStrategy mergeStrategy,
      DataContext ctx) {
    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try {
        ObjectNode existing = readForUpdate(conn, id, ctx);
        if (existing == null) {
          throw new RecordNotFoundException(collectionName, id);
        }

        int currentVersion = existing.get("_version").asInt();
        if (expectedVersion.isPresent() && currentVersion != expectedVersion.getAsInt()) {
          throw new VersionConflictException(
              collectionName, id, expectedVersion.getAsInt(), currentVersion);
        }

        ObjectNode merged = applyMerge(existing, stripSystemFields(changes), mergeStrategy);
        int newVersion = currentVersion + 1;

        try (PreparedStatement ps =
            conn.prepareStatement(
                "UPDATE data_records SET data = ?, _version = ?, updated_at = ?"
                    + " WHERE tenant_id = ? AND app_id = ? AND collection = ? AND doc_id = ?")) {
          ps.setString(1, MAPPER.writeValueAsString(stripSystemFields(merged)));
          ps.setInt(2, newVersion);
          ps.setString(3, Instant.now().toString());
          ps.setString(4, ctx.tenantId());
          ps.setString(5, ctx.appId());
          ps.setString(6, collectionName);
          ps.setString(7, id);
          ps.executeUpdate();
        }
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        throw e;
      }
    } catch (DataStoreException e) {
      throw e;
    } catch (Exception e) {
      throw new DataStoreException("Update failed", e);
    }
  }

  @Override
  public void delete(String id, DataContext ctx) {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "DELETE FROM data_records"
                    + " WHERE tenant_id = ? AND app_id = ? AND collection = ? AND doc_id = ?")) {
      ps.setString(1, ctx.tenantId());
      ps.setString(2, ctx.appId());
      ps.setString(3, collectionName);
      ps.setString(4, id);
      int rows = ps.executeUpdate();
      if (rows == 0) {
        throw new RecordNotFoundException(collectionName, id);
      }
    } catch (DataStoreException e) {
      throw e;
    } catch (SQLException e) {
      throw new DataStoreException("Delete failed", e);
    }
  }

  @Override
  public QueryResult query(Query query, DataContext ctx) {
    List<ObjectNode> all = listAll(ctx);
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
    List<ObjectNode> page = filtered.stream().skip(skip).limit(query.size()).toList();

    return new QueryResult(page, total, query.page(), query.size());
  }

  @Override
  public long count(Query query, DataContext ctx) {
    if (query.filters().isEmpty()) {
      try (Connection conn = dataSource.getConnection();
          PreparedStatement ps =
              conn.prepareStatement(
                  "SELECT COUNT(*) FROM data_records"
                      + " WHERE tenant_id = ? AND app_id = ? AND collection = ?")) {
        ps.setString(1, ctx.tenantId());
        ps.setString(2, ctx.appId());
        ps.setString(3, collectionName);
        try (ResultSet rs = ps.executeQuery()) {
          rs.next();
          return rs.getLong(1);
        }
      } catch (SQLException e) {
        throw new DataStoreException("Count failed", e);
      }
    }
    return query(query, ctx).totalCount();
  }

  @Override
  public List<ObjectNode> search(String text, DataContext ctx) {
    String lower = text.toLowerCase();
    return listAll(ctx).stream()
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
        .toList();
  }

  protected List<ObjectNode> listAll(DataContext ctx) {
    List<ObjectNode> results = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "SELECT data, doc_id, _version, created_at, updated_at FROM data_records"
                    + " WHERE tenant_id = ? AND app_id = ? AND collection = ?"
                    + " ORDER BY created_at")) {
      ps.setString(1, ctx.tenantId());
      ps.setString(2, ctx.appId());
      ps.setString(3, collectionName);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.add(toObjectNode(rs));
        }
      }
    } catch (SQLException | JsonProcessingException e) {
      throw new DataStoreException("List failed", e);
    }
    return results;
  }

  protected ObjectNode toObjectNode(ResultSet rs) throws SQLException, JsonProcessingException {
    ObjectNode node = (ObjectNode) MAPPER.readTree(rs.getString("data"));
    node.put("_id", rs.getString("doc_id"));
    node.put("_version", rs.getInt("_version"));
    node.put("_created_at", rs.getString("created_at"));
    node.put("_updated_at", rs.getString("updated_at"));
    return node;
  }

  private ObjectNode readForUpdate(Connection conn, String id, DataContext ctx)
      throws SQLException, JsonProcessingException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "SELECT data, doc_id, _version, created_at, updated_at FROM data_records"
                + " WHERE tenant_id = ? AND app_id = ? AND collection = ? AND doc_id = ?")) {
      ps.setString(1, ctx.tenantId());
      ps.setString(2, ctx.appId());
      ps.setString(3, collectionName);
      ps.setString(4, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return toObjectNode(rs);
        }
      }
    }
    return null;
  }

  private ObjectNode stripSystemFields(ObjectNode node) {
    ObjectNode copy = node.deepCopy();
    copy.remove("_id");
    copy.remove("_version");
    copy.remove("_created_at");
    copy.remove("_updated_at");
    return copy;
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
      default -> true;
    };
  }

  private ObjectNode applyMerge(ObjectNode existing, ObjectNode changes, MergeStrategy strategy) {
    ObjectNode clean = stripSystemFields(existing);
    return switch (strategy) {
      case REPLACE -> changes.deepCopy();
      case SHALLOW -> {
        ObjectNode merged = clean.deepCopy();
        changes.fields().forEachRemaining(f -> merged.set(f.getKey(), f.getValue()));
        yield merged;
      }
      case DEEP, RFC_7396 -> {
        ObjectNode merged = clean.deepCopy();
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
