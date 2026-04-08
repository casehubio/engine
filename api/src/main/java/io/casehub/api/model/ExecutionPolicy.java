package io.casehub.api.model;

public record ExecutionPolicy(Integer timeoutMs, RetryPolicy retries) {

    public ExecutionPolicy() {
        this(60000, new RetryPolicy());
    }

}
