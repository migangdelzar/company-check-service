package com.incode.verification.application.context;

import java.util.Objects;
import java.util.UUID;

/** Request metadata available to application code without framework coupling. */
public record ExecutionContext(UUID correlationId) {
    public static final ScopedValue<ExecutionContext> CURRENT = ScopedValue.newInstance();

    public ExecutionContext {
        Objects.requireNonNull(correlationId, "correlationId");
    }

    public static ExecutionContext current() {
        return CURRENT.orElseThrow(() -> new IllegalStateException("no execution context is bound"));
    }
}
