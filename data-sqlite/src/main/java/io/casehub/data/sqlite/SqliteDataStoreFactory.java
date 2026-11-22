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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;

public class SqliteDataStoreFactory {

  public static SqliteDocumentDataStore createInMemory() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite::memory:");
    config.setMaximumPoolSize(1);
    HikariDataSource ds = new HikariDataSource(config);
    migrate(ds);
    return new SqliteDocumentDataStore(ds);
  }

  public static SqliteTypedDataStore createTypedInMemory() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite::memory:");
    config.setMaximumPoolSize(1);
    HikariDataSource ds = new HikariDataSource(config);
    migrate(ds);
    return new SqliteTypedDataStore(ds);
  }

  public static SqliteDocumentDataStore create(String dbPath) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + dbPath);
    config.setMaximumPoolSize(1);
    config.addDataSourceProperty("journal_mode", "WAL");
    config.addDataSourceProperty("busy_timeout", "5000");
    HikariDataSource ds = new HikariDataSource(config);
    migrate(ds);
    return new SqliteDocumentDataStore(ds);
  }

  private static void migrate(DataSource ds) {
    try (Connection conn = ds.getConnection();
        Statement stmt = conn.createStatement()) {
      var stream =
          SqliteDataStoreFactory.class.getResourceAsStream(
              "/db/data-store/migration/V1__create_data_records.sql");
      if (stream == null) {
        throw new IllegalStateException("Migration resource not found on classpath");
      }
      String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty()) {
          stmt.executeUpdate(trimmed);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to migrate data store", e);
    }
  }
}
