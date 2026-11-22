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
package io.casehub.platform.api.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

public abstract class DataStoreContractTest {

  protected static final ObjectMapper MAPPER = new ObjectMapper();

  protected abstract DataStore dataStore();

  protected abstract DataContext testContext();

  protected ObjectNode record(String... kvPairs) {
    ObjectNode node = MAPPER.createObjectNode();
    for (int i = 0; i < kvPairs.length; i += 2) {
      node.put(kvPairs[i], kvPairs[i + 1]);
    }
    return node;
  }

  @Test
  void insert_and_get_roundTrip() {
    DataCollection col = dataStore().collection("test");
    ObjectNode rec = record("name", "Alice", "age", "30");
    String id = col.insert(rec, testContext());

    assertThat(id).isNotNull();
    ObjectNode result = col.get(id, testContext()).orElseThrow();
    assertThat(result.get("name").asText()).isEqualTo("Alice");
    assertThat(result.get("age").asText()).isEqualTo("30");
    assertThat(result.get("_id").asText()).isEqualTo(id);
    assertThat(result.get("_version").asInt()).isEqualTo(1);
    assertThat(result.has("_created_at")).isTrue();
    assertThat(result.has("_updated_at")).isTrue();
  }

  @Test
  void insert_withExplicitId_usesProvidedId() {
    DataCollection col = dataStore().collection("test");
    ObjectNode rec = record("name", "Alice");
    String id = col.insert("custom-id-123", rec, testContext());

    assertThat(id).isEqualTo("custom-id-123");
    ObjectNode result = col.get("custom-id-123", testContext()).orElseThrow();
    assertThat(result.get("name").asText()).isEqualTo("Alice");
    assertThat(result.get("_id").asText()).isEqualTo("custom-id-123");
  }

  @Test
  void get_nonExistent_returnsEmpty() {
    DataCollection col = dataStore().collection("test");
    assertThat(col.get("nonexistent", testContext())).isEmpty();
  }

  @Test
  void update_incrementsVersion() {
    DataCollection col = dataStore().collection("test");
    String id = col.insert(record("name", "Alice"), testContext());

    col.update(
        id, record("name", "Bob"), OptionalInt.empty(), MergeStrategy.REPLACE, testContext());

    ObjectNode result = col.get(id, testContext()).orElseThrow();
    assertThat(result.get("name").asText()).isEqualTo("Bob");
    assertThat(result.get("_version").asInt()).isEqualTo(2);
  }

  @Test
  void update_withExpectedVersion_throwsOnConflict() {
    DataCollection col = dataStore().collection("test");
    String id = col.insert(record("name", "Alice"), testContext());

    assertThatThrownBy(
            () ->
                col.update(
                    id,
                    record("name", "Bob"),
                    OptionalInt.of(99),
                    MergeStrategy.REPLACE,
                    testContext()))
        .isInstanceOf(VersionConflictException.class);
  }

  @Test
  void update_withCorrectExpectedVersion_succeeds() {
    DataCollection col = dataStore().collection("test");
    String id = col.insert(record("name", "Alice"), testContext());

    col.update(id, record("name", "Bob"), OptionalInt.of(1), MergeStrategy.REPLACE, testContext());

    ObjectNode result = col.get(id, testContext()).orElseThrow();
    assertThat(result.get("name").asText()).isEqualTo("Bob");
  }

  @Test
  void update_nonExistent_throws() {
    DataCollection col = dataStore().collection("test");
    assertThatThrownBy(
            () ->
                col.update(
                    "nonexistent",
                    record("name", "Bob"),
                    OptionalInt.empty(),
                    MergeStrategy.REPLACE,
                    testContext()))
        .isInstanceOf(RecordNotFoundException.class);
  }

  @Test
  void delete_removesRecord() {
    DataCollection col = dataStore().collection("test");
    String id = col.insert(record("name", "Alice"), testContext());

    col.delete(id, testContext());
    assertThat(col.get(id, testContext())).isEmpty();
  }

  @Test
  void query_filtersByField() {
    DataCollection col = dataStore().collection("test");
    col.insert(record("status", "ACTIVE"), testContext());
    col.insert(record("status", "CLOSED"), testContext());
    col.insert(record("status", "ACTIVE"), testContext());

    Query q = new Query(List.of(new Filter("status", FilterOp.EQ, "ACTIVE")), List.of(), 0, 20);
    QueryResult result = col.query(q, testContext());
    assertThat(result.records()).hasSize(2);
    assertThat(result.totalCount()).isEqualTo(2);
  }

  @Test
  void query_pagination() {
    DataCollection col = dataStore().collection("test");
    for (int i = 0; i < 5; i++) {
      col.insert(record("idx", String.valueOf(i)), testContext());
    }

    Query q = new Query(List.of(), List.of(), 0, 2);
    QueryResult page1 = col.query(q, testContext());
    assertThat(page1.records()).hasSize(2);
    assertThat(page1.totalCount()).isEqualTo(5);

    Query q2 = new Query(List.of(), List.of(), 1, 2);
    QueryResult page2 = col.query(q2, testContext());
    assertThat(page2.records()).hasSize(2);
  }

  @Test
  void query_sorting() {
    DataCollection col = dataStore().collection("test");
    col.insert(record("name", "Charlie"), testContext());
    col.insert(record("name", "Alice"), testContext());
    col.insert(record("name", "Bob"), testContext());

    Query q = new Query(List.of(), List.of(new SortField("name", SortDirection.ASC)), 0, 20);
    QueryResult result = col.query(q, testContext());
    assertThat(result.records().get(0).get("name").asText()).isEqualTo("Alice");
    assertThat(result.records().get(1).get("name").asText()).isEqualTo("Bob");
    assertThat(result.records().get(2).get("name").asText()).isEqualTo("Charlie");
  }

  @Test
  void count_returnsTotal() {
    DataCollection col = dataStore().collection("test");
    col.insert(record("x", "1"), testContext());
    col.insert(record("x", "2"), testContext());
    col.insert(record("x", "3"), testContext());

    assertThat(col.count(Query.all(), testContext())).isEqualTo(3);
  }

  @Test
  void schemaless_collection_worksWithoutRegistration() {
    DataCollection col = dataStore().collection("unregistered");
    String id = col.insert(record("anything", "goes"), testContext());
    ObjectNode result = col.get(id, testContext()).orElseThrow();
    assertThat(result.get("anything").asText()).isEqualTo("goes");
  }

  @Test
  void systemFields_injectedOnRead_strippedOnWrite() {
    DataCollection col = dataStore().collection("test");
    ObjectNode rec = record("name", "Alice");
    rec.put("_id", "should-be-ignored");
    rec.put("_version", 999);

    String id = col.insert(rec, testContext());
    ObjectNode result = col.get(id, testContext()).orElseThrow();
    assertThat(result.get("_version").asInt()).isEqualTo(1);
    assertThat(result.get("_id").asText()).isEqualTo(id);
  }

  @Test
  void returnedNodes_areDefensiveCopies() {
    DataCollection col = dataStore().collection("test");
    String id = col.insert(record("name", "Alice"), testContext());

    ObjectNode result1 = col.get(id, testContext()).orElseThrow();
    result1.put("name", "MUTATED");

    ObjectNode result2 = col.get(id, testContext()).orElseThrow();
    assertThat(result2.get("name").asText()).isEqualTo("Alice");
  }

  @Test
  void tenant_isolation() {
    DataCollection col = dataStore().collection("test");
    DataContext tenant1 = new DataContext("tenant1", testContext().appId());
    DataContext tenant2 = new DataContext("tenant2", testContext().appId());

    col.insert(record("name", "Alice"), tenant1);
    col.insert(record("name", "Bob"), tenant2);

    assertThat(col.count(Query.all(), tenant1)).isEqualTo(1);
    assertThat(col.count(Query.all(), tenant2)).isEqualTo(1);
  }

  @Test
  void merge_shallow() {
    DataCollection col = dataStore().collection("test");
    String id = col.insert(record("name", "Alice", "age", "30"), testContext());

    col.update(
        id, record("name", "Bob"), OptionalInt.empty(), MergeStrategy.SHALLOW, testContext());

    ObjectNode result = col.get(id, testContext()).orElseThrow();
    assertThat(result.get("name").asText()).isEqualTo("Bob");
    assertThat(result.get("age").asText()).isEqualTo("30");
  }
}
