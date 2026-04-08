package io.casehub.engine.internal.util;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

import java.util.function.Supplier;

public final class ReactiveUtils {

    private ReactiveUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Uni<T> runOnSafeVertxContext(Vertx vertx, Supplier<Uni<? extends T>> action) {
        return Uni.createFrom().<T>deferred(() -> (Uni<T>) action.get())
                .runSubscriptionOn(command -> {
                    Context dc = VertxContext.getOrCreateDuplicatedContext(vertx);
                    VertxContextSafetyToggle.setContextSafe(dc, true);
                    dc.runOnContext(v -> command.run());
                });
    }
}
