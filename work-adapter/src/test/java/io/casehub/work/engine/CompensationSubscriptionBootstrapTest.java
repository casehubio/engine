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
package io.casehub.work.engine;

import io.casehub.platform.api.notification.NotificationSeverity;
import io.casehub.platform.api.subscription.EventTypeDescriptor;
import io.casehub.platform.api.subscription.EventTypeRegistry;
import io.casehub.platform.api.subscription.Subscription;
import io.casehub.platform.api.subscription.SubscriptionInput;
import io.casehub.platform.api.subscription.SubscriptionStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompensationSubscriptionBootstrapTest {

    @Test
    void registersAllFiveSubscriptions() {
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.empty());
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();

        var captor = ArgumentCaptor.forClass(SubscriptionInput.class);
        verify(store, times(5)).store(captor.capture());
        var eventTypes = captor.getAllValues().stream().map(SubscriptionInput::eventType).toList();
        assertThat(eventTypes)
                .containsExactlyInAnyOrder(
                        "io.casehub.work.workitem.compensation_started",
                        "io.casehub.work.workitem.compensation_completed",
                        "io.casehub.engine.case.compensation.started",
                        "io.casehub.engine.case.compensation.completed",
                        "io.casehub.engine.case.compensation.faulted");
    }

    @Test
    void skipsAlreadyRegisteredSubscriptions() {
        var existing = mock(Subscription.class);
        when(existing.eventType()).thenReturn("io.casehub.engine.case.compensation.started");
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.of(existing));
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();

        var captor = ArgumentCaptor.forClass(SubscriptionInput.class);
        verify(store, times(4)).store(captor.capture());
        var eventTypes = captor.getAllValues().stream().map(SubscriptionInput::eventType).toList();
        assertThat(eventTypes).doesNotContain("io.casehub.engine.case.compensation.started");
    }

    @Test
    void registersThreeCaseEventTypeDescriptors() {
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.empty());
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();

        var captor = ArgumentCaptor.forClass(EventTypeDescriptor.class);
        verify(eventRegistry, times(3)).register(captor.capture());
        var eventTypes = captor.getAllValues().stream().map(EventTypeDescriptor::eventType).toList();
        assertThat(eventTypes)
                .containsExactlyInAnyOrder(
                        "io.casehub.engine.case.compensation.started",
                        "io.casehub.engine.case.compensation.completed",
                        "io.casehub.engine.case.compensation.faulted");
    }

    @Test
    void caseCompensationStarted_hasUrgentSeverity() {
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.empty());
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();

        var captor = ArgumentCaptor.forClass(SubscriptionInput.class);
        verify(store, times(5)).store(captor.capture());
        var started =
                captor.getAllValues().stream()
                      .filter(s -> s.eventType().equals("io.casehub.engine.case.compensation.started"))
                      .findFirst()
                      .orElseThrow();
        assertThat(started.template().severity()).isEqualTo(NotificationSeverity.URGENT);
    }

    @Test
    void caseCompensationFaulted_hasUrgentSeverity() {
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.empty());
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();

        var captor = ArgumentCaptor.forClass(SubscriptionInput.class);
        verify(store, times(5)).store(captor.capture());
        var faulted =
                captor.getAllValues().stream()
                      .filter(s -> s.eventType().equals("io.casehub.engine.case.compensation.faulted"))
                      .findFirst()
                      .orElseThrow();
        assertThat(faulted.template().severity()).isEqualTo(NotificationSeverity.URGENT);
    }

    @Test
    void workCompensationCompleted_hasInfoSeverity() {
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.empty());
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();

        var captor = ArgumentCaptor.forClass(SubscriptionInput.class);
        verify(store, times(5)).store(captor.capture());
        var completed =
                captor.getAllValues().stream()
                      .filter(s -> s.eventType().equals("io.casehub.work.workitem.compensation_completed"))
                      .findFirst()
                      .orElseThrow();
        assertThat(completed.template().severity()).isEqualTo(NotificationSeverity.INFO);
    }

    @Test
    void noOp_whenStoreEmpty() {
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.empty(), Optional.empty());

        bootstrap.init();
    }

    @Test
    void handlesStoreFailureGracefully() {
        var store = mock(SubscriptionStore.class);
        when(store.findAllEnabled()).thenReturn(Stream.empty());
        when(store.store(any())).thenThrow(new RuntimeException("DB down"));
        var eventRegistry = mock(EventTypeRegistry.class);
        var bootstrap = new CompensationSubscriptionBootstrap(
                Optional.of(store), Optional.of(eventRegistry));

        bootstrap.init();
    }
}
