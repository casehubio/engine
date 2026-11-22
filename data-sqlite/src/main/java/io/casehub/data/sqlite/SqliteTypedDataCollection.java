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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.platform.api.data.CollectionDescriptor;
import io.casehub.platform.api.data.DataContext;
import io.casehub.platform.api.data.DataStoreException;
import io.casehub.platform.api.data.Filter;
import io.casehub.platform.api.data.Query;
import io.casehub.platform.api.data.QueryResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;

public class SqliteTypedDataCollection extends SqliteDocumentDataCollection {

  private static final Pattern SAFE_FIELD_NAME = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
  private final CollectionDescriptor descriptor;

  SqliteTypedDataCollection(
      DataSource dataSource, String collectionName, CollectionDescriptor descriptor) {
    super(dataSource, collectionName);
    this.descriptor = descriptor;
  }

  @Override
  public QueryResult query(Query query, DataContext ctx) {
    if (query.filters().isEmpty() && query.sort().isEmpty()) {
      return super.query(query, ctx);
    }
    return queryWithJsonExtract(query, ctx);
  }

  @Override
  public long count(Query query, DataContext ctx) {
    if (query.filters().isEmpty()) {
      return super.count(query, ctx);
    }
    return countWithJsonExtract(query, ctx);
  }

  private QueryResult queryWithJsonExtract(Query query, DataContext ctx) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT data, doc_id, _version, created_at, updated_at FROM data_records"
                + " WHERE tenant_id = ? AND app_id = ? AND collection = ?");
    List<Object> params = new ArrayList<>();
    params.add(ctx.tenantId());
    params.add(ctx.appId());
    params.add(getCollectionName());

    appendFilters(sql, params, query.filters());

    long total = countWithSql(toCountSql(sql.toString()), params);

    if (!query.sort().isEmpty()) {
      sql.append(" ORDER BY ");
      sql.append(
          query.sort().stream()
              .map(
                  sf ->
                      "json_extract(data, '$."
                          + validateFieldName(sf.field())
                          + "') "
                          + sf.direction().name())
              .collect(Collectors.joining(", ")));
    }

    sql.append(" LIMIT ? OFFSET ?");
    params.add(query.size());
    params.add(query.page() * query.size());

    List<ObjectNode> records = executeQuery(sql.toString(), params);
    return new QueryResult(records, total, query.page(), query.size());
  }

  private long countWithJsonExtract(Query query, DataContext ctx) {
    StringBuilder sql =
        new StringBuilder(
            "SELECT COUNT(*) FROM data_records"
                + " WHERE tenant_id = ? AND app_id = ? AND collection = ?");
    List<Object> params = new ArrayList<>();
    params.add(ctx.tenantId());
    params.add(ctx.appId());
    params.add(getCollectionName());

    appendFilters(sql, params, query.filters());

    return countWithSql(sql.toString(), params);
  }

  private void appendFilters(StringBuilder sql, List<Object> params, List<Filter> filters) {
    for (Filter f : filters) {
      String field = validateFieldName(f.field());
      sql.append(" AND json_extract(data, '$.").append(field).append("')");
      switch (f.op()) {
        case EQ -> {
          sql.append(" = ?");
          params.add(f.value().toString());
        }
        case NEQ -> {
          sql.append(" != ?");
          params.add(f.value().toString());
        }
        case GT -> {
          sql.append(" > ?");
          params.add(f.value().toString());
        }
        case GTE -> {
          sql.append(" >= ?");
          params.add(f.value().toString());
        }
        case LT -> {
          sql.append(" < ?");
          params.add(f.value().toString());
        }
        case LTE -> {
          sql.append(" <= ?");
          params.add(f.value().toString());
        }
        case CONTAINS -> {
          sql.append(" LIKE ?");
          params.add("%" + f.value() + "%");
        }
        default -> {}
      }
    }
  }

  private static String validateFieldName(String field) {
    if (!SAFE_FIELD_NAME.matcher(field).matches()) {
      throw new DataStoreException("Invalid field name: " + field);
    }
    return field;
  }

  private String toCountSql(String selectSql) {
    return selectSql.replace(
        "SELECT data, doc_id, _version, created_at, updated_at", "SELECT COUNT(*)");
  }

  private long countWithSql(String sql, List<Object> params) {
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (Exception e) {
      throw new DataStoreException("Count query failed", e);
    }
  }

  private List<ObjectNode> executeQuery(String sql, List<Object> params) {
    List<ObjectNode> results = new ArrayList<>();
    try (Connection conn = getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          results.add(toObjectNode(rs));
        }
      }
    } catch (Exception e) {
      throw new DataStoreException("Query failed", e);
    }
    return results;
  }
}
