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
package io.casehub.engine;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QueryCaseHubInstanceTest {

  @Inject SimpleCaseHubBean bean;

  @Test
  public void testQueryStringValue() {
    UUID caseId =
        startCaseAndAwait(
            Map.of(
                "documentId", "doc-123",
                "status", "processing"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object result =
                  bean.query(caseId, "documentId").toCompletableFuture().get(1, TimeUnit.SECONDS);
              assertNotNull(result);
              assertEquals("doc-123", result.toString());
            });
  }

  @Test
  public void testQueryNonExistentPathReturnsNull() {
    UUID caseId =
        startCaseAndAwait(
            Map.of(
                "documentId", "doc-200",
                "status", "processing"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object result =
                  bean.query(caseId, "nonExistentKey")
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNull(result);
            });
  }

  @Test
  public void testQueryWithTypedCast() {
    UUID caseId =
        startCaseAndAwait(
            Map.of(
                "documentId", "doc-300",
                "status", "processing"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              String result =
                  bean.query(caseId, "documentId", String.class)
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertEquals("doc-300", result);
            });
  }

  @Test
  public void testQueryTypedCastReturnsNullForMissingPath() {
    UUID caseId =
        startCaseAndAwait(
            Map.of(
                "documentId", "doc-400",
                "status", "processing"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              String result =
                  bean.query(caseId, "missingKey", String.class)
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNull(result);
            });
  }

  @Test
  public void testQueryTypedCastWrongTypeThrowsClassCastException() {
    UUID caseId =
        startCaseAndAwait(
            Map.of(
                "documentId", "doc-500",
                "status", "processing"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              ExecutionException ex =
                  assertThrows(
                      ExecutionException.class,
                      () ->
                          bean.query(caseId, "documentId", Integer.class)
                              .toCompletableFuture()
                              .get(1, TimeUnit.SECONDS));
              assertInstanceOf(ClassCastException.class, ex.getCause());
            });
  }

  @Test
  public void testQueryNestedPath() {
    Map<String, Object> context = new HashMap<>();
    context.put("documentId", "doc-600");
    context.put("status", "processing");
    context.put("metadata", Map.of("author", "Alice", "version", "1.0"));

    UUID caseId = startCaseAndAwait(context);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object nested =
                  bean.query(caseId, "metadata.author")
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNotNull(nested);
              assertEquals("Alice", nested.toString());
            });

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object version =
                  bean.query(caseId, "metadata.version")
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNotNull(version);
              assertEquals("1.0", version.toString());
            });
  }

  @Test
  public void testQueryNestedPathMissingIntermediateReturnsNull() {
    UUID caseId =
        startCaseAndAwait(
            Map.of(
                "documentId", "doc-700",
                "status", "processing"));

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object result =
                  bean.query(caseId, "nonExistent.deeply.nested.path")
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNull(result);
            });
  }

  @Test
  public void testQueryNonExistentCaseThrowsException() {
    UUID fakeCaseId = UUID.randomUUID();

    ExecutionException ex =
        assertThrows(
            ExecutionException.class,
            () ->
                bean.query(fakeCaseId, "documentId")
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS));
    assertInstanceOf(RuntimeException.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains(fakeCaseId.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryReturnsMapForNestedObject() {
    Map<String, Object> context = new HashMap<>();
    context.put("documentId", "doc-800");
    context.put("status", "processing");
    context.put("details", Map.of("type", "invoice", "amount", 100));

    UUID caseId = startCaseAndAwait(context);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Map<String, Object> details =
                  bean.query(caseId, "details", Map.class)
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNotNull(details);
              assertEquals("invoice", details.get("type"));
              assertEquals(100, details.get("amount"));
            });
  }

  @Test
  public void testQueryWithNumericContextValue() {
    Map<String, Object> context = new HashMap<>();
    context.put("documentId", "doc-900");
    context.put("status", "processing");
    context.put("priority", 42);

    UUID caseId = startCaseAndAwait(context);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object result =
                  bean.query(caseId, "priority").toCompletableFuture().get(1, TimeUnit.SECONDS);
              assertNotNull(result);
              assertEquals(42, result);
            });
  }

  @Test
  public void testQueryWithBooleanContextValue() {
    Map<String, Object> context = new HashMap<>();
    context.put("documentId", "doc-1000");
    context.put("status", "processing");
    context.put("urgent", true);

    UUID caseId = startCaseAndAwait(context);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Boolean result =
                  bean.query(caseId, "urgent", Boolean.class)
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNotNull(result);
              assertTrue(result);
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryWithListContextValue() {
    Map<String, Object> context = new HashMap<>();
    context.put("documentId", "doc-1100");
    context.put("status", "processing");
    context.put("tags", List.of("urgent", "finance", "review"));

    UUID caseId = startCaseAndAwait(context);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              List<String> tags =
                  bean.query(caseId, "tags", List.class)
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNotNull(tags);
              assertEquals(3, tags.size());
              assertTrue(tags.contains("urgent"));
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testQueryWithNestedMapContextValue() {
    Map<String, Object> context = new HashMap<>();
    context.put("documentId", "doc-1200");
    context.put("status", "processing");
    context.put("metadata", Map.of("author", "John", "department", "finance"));

    UUID caseId = startCaseAndAwait(context);

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Object author =
                  bean.query(caseId, "metadata.author")
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertEquals("John", author);
            });

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              Map<String, Object> metadata =
                  bean.query(caseId, "metadata", Map.class)
                      .toCompletableFuture()
                      .get(1, TimeUnit.SECONDS);
              assertNotNull(metadata);
              assertEquals("John", metadata.get("author"));
              assertEquals("finance", metadata.get("department"));
            });
  }

  private UUID startCaseAndAwait(Map<String, Object> initialContext) {
    AtomicReference<UUID> ref = new AtomicReference<>();
    AtomicReference<Throwable> err = new AtomicReference<>();

    bean.startCase(initialContext)
        .thenAccept(ref::set)
        .exceptionally(
            ex -> {
              err.set(ex);
              return null;
            });

    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              if (err.get() != null) throw new AssertionError(err.get());
              assertNotNull(ref.get());
            });

    return ref.get();
  }
}
