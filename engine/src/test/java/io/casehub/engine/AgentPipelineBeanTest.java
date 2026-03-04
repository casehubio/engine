package io.casehub.engine;

import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.model.CaseState;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AgentPipelineBeanTest {

  @Inject
  AgentPipelineBean bean;

  @Inject
  CaseInstanceCache caseInstanceCache;

  MockWebServer mockServer;

  @BeforeEach
  void setUp() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start(8889);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockServer.shutdown();
  }

  @Test
  public void testAgentPipeline() {
    mockServer.enqueue(new MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody("{\"content\":\"Our new AI platform leverages cutting-edge technology.\",\"source\":\"api\"}"));

    Agents.SentimentAnalysisAgent sentimentMock = mock(Agents.SentimentAnalysisAgent.class);
    when(sentimentMock.analyze(anyString(), any(Agents.SentimentRequest.class)))
            .thenAnswer(invocation -> new Agents.SentimentResult(
                    "positive", 0.92, List.of("innovative", "growth", "promising")));

    Agents.ContentSummarizerAgent summarizerMock = mock(Agents.ContentSummarizerAgent.class);
    when(summarizerMock.summarize(anyString(), any(Agents.SummaryRequest.class)))
            .thenAnswer(invocation -> new Agents.SummaryResult(
                    "The document describes an innovative approach to technology with strong growth potential.",
                    List.of("Innovation driven", "High growth potential", "Market-ready solution")));

    bean.setAgents(sentimentMock, summarizerMock);

    Map<String, Object> initialContext = Map.of(
            "documentId", "doc-agent-1",
            "step", "submitted"
    );

    AtomicReference<UUID> ref = new AtomicReference<>();
    AtomicReference<Throwable> err = new AtomicReference<>();

    bean.startCase(initialContext)
            .thenAccept(ref::set)
            .exceptionally(ex -> { err.set(ex); return null; });

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      if (err.get() != null) throw new AssertionError(err.get());
      assertNotNull(ref.get());
    });

    await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
      var instance = caseInstanceCache.get(ref.get());
      assertNotNull(instance);
      assertEquals(CaseState.COMPLETED, instance.getState());
    });

    assertEquals(1, mockServer.getRequestCount());

    verify(sentimentMock).analyze(anyString(), any(Agents.SentimentRequest.class));
    verify(summarizerMock).summarize(anyString(), any(Agents.SummaryRequest.class));
  }
}
