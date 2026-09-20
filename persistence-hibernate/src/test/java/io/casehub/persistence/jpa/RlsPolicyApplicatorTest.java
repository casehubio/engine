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

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class RlsPolicyApplicatorTest {

  @Test
  void onStart_whenDisabled_doesNothing() {
    DataSource ds = mock(DataSource.class);
    var setup = new RlsPolicySetup(ds, false);
    var applicator = new RlsPolicyApplicator();
    applicator.setup = setup;

    applicator.onStart(null);
    verifyNoInteractions(ds);
  }

  @Test
  void onStart_whenEnabled_opensConnectionAndAppliesRls() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    Statement stmt = mock(Statement.class);
    when(ds.getConnection()).thenReturn(conn);
    when(conn.createStatement()).thenReturn(stmt);

    var setup = new RlsPolicySetup(ds, true);
    var applicator = new RlsPolicyApplicator();
    applicator.setup = setup;

    applicator.onStart(null);

    verify(ds).getConnection();
    verify(stmt, atLeast(16)).execute(anyString());
  }
}
