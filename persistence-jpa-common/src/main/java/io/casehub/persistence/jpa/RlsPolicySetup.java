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
package io.casehub.persistence.jpa;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.jboss.logging.Logger;

public class RlsPolicySetup {

  private static final Logger LOG = Logger.getLogger(RlsPolicySetup.class);

  static final List<String> TABLES =
      List.of("case_instance", "case_meta_model", "event_log", "plan_item", "subcase_group");

  private final DataSource dataSource;
  private final boolean rlsEnabled;

  public RlsPolicySetup(DataSource dataSource, boolean rlsEnabled) {
    this.dataSource = dataSource;
    this.rlsEnabled = rlsEnabled;
  }

  public void apply() {
    if (!rlsEnabled) {
      LOG.debug("RLS disabled (casehub.rls.enabled=false) — skipping policy application");
      return;
    }
    LOG.info("Applying PostgreSQL Row Level Security policies");
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      createBypassRole(stmt);
      for (String table : TABLES) {
        applyRls(stmt, table);
      }
      LOG.infof("RLS applied to %d tables", TABLES.size());
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to apply RLS policies", e);
    }
  }

  private void createBypassRole(Statement stmt) throws SQLException {
    stmt.execute(
        "DO $$ BEGIN "
            + "  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'casehub_crosstenancy') THEN"
            + "    EXECUTE 'CREATE ROLE casehub_crosstenancy BYPASSRLS'; "
            + "  END IF; "
            + "END $$");
    stmt.execute("GRANT casehub_crosstenancy TO current_user");
    for (String table : TABLES) {
      stmt.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON " + table + " TO casehub_crosstenancy");
    }
    stmt.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO casehub_crosstenancy");
  }

  private void applyRls(Statement stmt, String table) throws SQLException {
    stmt.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
    stmt.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
    stmt.execute(
        "DO $$ BEGIN "
            + "  IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE tablename = '"
            + table
            + "' AND policyname = 'tenant_isolation') THEN "
            + "    EXECUTE 'CREATE POLICY tenant_isolation ON "
            + table
            + " USING (tenancy_id = current_setting(''casehub.tenancy_id'', true))'; "
            + "  END IF; "
            + "END $$");
  }
}
