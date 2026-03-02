package io.casehub.engine;

import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.model.CaseState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class SimpleCaseHubBeanTest {

  @Inject
  SimpleCaseHubBean bean;

  @Inject
  CaseInstanceCache caseInstanceCache;

  @Test
  public void testSimpleCaseHubBean() {
    AtomicReference<UUID> ref = new AtomicReference<>();
    AtomicReference<Throwable> err = new AtomicReference<>();

    Map<String, Object> initialContext = Map.of(
            "documentId", "doc-123",
            "status", "processing"
    );

    bean.startCase(initialContext)
            .thenAccept(ref::set)
            .exceptionally(ex -> { err.set(ex); return null; });

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      if (err.get() != null) throw new AssertionError(err.get());
      assertNotNull(ref.get());
    });

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      var instance = caseInstanceCache.get(ref.get());
      assertNotNull(instance);
      assertEquals(CaseState.COMPLETED, instance.getState());
    });
  }
}
