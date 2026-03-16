package io.casehub.api.model;

public record RetryPolicy(Integer maxAttempts, Integer delayMs) {

    public RetryPolicy() {
        this(3, 10000);
    }

}
