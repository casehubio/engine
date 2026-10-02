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
package io.casehub.engine.internal.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.engine.ExpressionEngineRegistry;
import io.casehub.api.model.CaseDefinition;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.spi.CaseMetaModelRepository;
import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.enterprise.inject.Instance;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultCaseDefinitionRegistryConcurrencyTest {

  @Test
  void lookupCannotObservePartiallyRegisteredDefinitions() throws Exception {
    CaseDefinition first = definition("first");
    CaseDefinition second = definition("second");
    CaseHub firstHub = caseHub(first);
    CaseHub secondHub = caseHub(second);

    @SuppressWarnings("unchecked")
    Instance<CaseHub> hubs = mock(Instance.class);
    when(hubs.iterator()).thenAnswer(ignored -> List.of(firstHub, secondHub).iterator());

    CountDownLatch firstDefinitionReachedRepository = new CountDownLatch(1);
    CountDownLatch registrationCanProceed = new CountDownLatch(1);
    AtomicInteger repositoryLookups = new AtomicInteger();
    CaseMetaModelRepository repository = mock(CaseMetaModelRepository.class);
    when(repository.findByKey(anyString(), anyString(), anyString(), anyString()))
        .thenAnswer(
            ignored -> {
              if (repositoryLookups.incrementAndGet() == 1) {
                firstDefinitionReachedRepository.countDown();
                assertThat(registrationCanProceed.await(2, TimeUnit.SECONDS)).isTrue();
              }
              return Optional.empty();
            });
    when(repository.save(org.mockito.ArgumentMatchers.any(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DefaultCaseDefinitionRegistry registry = new DefaultCaseDefinitionRegistry();
    registry.caseHubInstance = hubs;
    registry.caseMetaModelRepository = repository;
    registry.expressionEngineRegistry = mock(ExpressionEngineRegistry.class);
    registry.currentPrincipal = mock(CurrentPrincipal.class);
    when(registry.currentPrincipal.tenancyId()).thenReturn("tenant");
    @SuppressWarnings("unchecked")
    Instance<VocabularyRegistry> vocabularies = mock(Instance.class);
    when(vocabularies.isResolvable()).thenReturn(false);
    registry.vocabularyRegistry = vocabularies;

    CaseMetaModel secondMetaModel = new CaseMetaModel();
    secondMetaModel.setNamespace(second.getNamespace());
    secondMetaModel.setName(second.getName());
    secondMetaModel.setVersion(second.getVersion());

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      CompletableFuture<Void> registration =
          CompletableFuture.runAsync(registry::registerKnownDefinitions, executor);
      assertThat(firstDefinitionReachedRepository.await(2, TimeUnit.SECONDS)).isTrue();

      CompletableFuture<CaseDefinition> lookup =
          CompletableFuture.supplyAsync(
              () -> registry.getCaseDefinition(secondMetaModel), executor);
      Thread.sleep(100);
      assertThat(lookup).isNotDone();

      registrationCanProceed.countDown();

      registration.get(2, TimeUnit.SECONDS);
      assertThat(lookup.get(2, TimeUnit.SECONDS)).isSameAs(second);
    }
  }

  private static CaseDefinition definition(String name) {
    return CaseDefinition.builder().namespace("concurrency-test").name(name).version("1.0").build();
  }

  private static CaseHub caseHub(CaseDefinition definition) {
    CaseHub hub = mock(CaseHub.class);
    when(hub.getDefinition()).thenReturn(definition);
    return hub;
  }
}
