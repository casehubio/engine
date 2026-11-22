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

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.platform.api.data.CollectionDescriptor;
import io.casehub.platform.api.data.DataCollection;
import io.casehub.platform.api.data.DataContext;
import io.casehub.platform.api.data.DataStore;
import io.casehub.platform.api.data.DataStoreContractTest;
import io.casehub.platform.api.data.FieldDescriptor;
import io.casehub.platform.api.data.FieldType;
import io.casehub.platform.api.data.Filter;
import io.casehub.platform.api.data.FilterOp;
import io.casehub.platform.api.data.IndexDescriptor;
import io.casehub.platform.api.data.IndexType;
import io.casehub.platform.api.data.Query;
import io.casehub.platform.api.data.QueryResult;
import io.casehub.platform.api.data.StorageHint;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteTypedDataStoreContractTest extends DataStoreContractTest {

  private SqliteTypedDataStore store;

  @BeforeEach
  void init() {
    store = SqliteDataStoreFactory.createTypedInMemory();
  }

  @AfterEach
  void cleanup() {
    store.close();
  }

  @Override
  protected DataStore dataStore() {
    return store;
  }

  @Override
  protected DataContext testContext() {
    return new DataContext("test-tenant", "test-app");
  }

  @Test
  void registered_collection_usesIndexedColumns() {
    store.register(
        new CollectionDescriptor(
            "typed_col",
            List.of(
                new FieldDescriptor("status", FieldType.STRING, true, null),
                new FieldDescriptor("priority", FieldType.INTEGER, false, null)),
            List.of(new IndexDescriptor("idx_status", List.of("status"), IndexType.STANDARD)),
            List.of(),
            StorageHint.TYPED));

    DataCollection col = store.collection("typed_col");
    col.insert(record("status", "OPEN", "priority", "1"), testContext());
    col.insert(record("status", "CLOSED", "priority", "2"), testContext());

    Query q = new Query(List.of(new Filter("status", FilterOp.EQ, "OPEN")), List.of(), 0, 20);
    QueryResult result = col.query(q, testContext());
    assertThat(result.records()).hasSize(1);
    assertThat(result.records().get(0).get("status").asText()).isEqualTo("OPEN");
  }
}
