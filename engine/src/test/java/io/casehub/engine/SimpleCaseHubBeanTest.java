package io.casehub.engine;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class SimpleCaseHubBeanTest {

  @Inject
  SimpleCaseHubBean bean;


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
      org.junit.jupiter.api.Assertions.assertNotNull(ref.get());
    });
  }
}
