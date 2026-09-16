package com.incode.verification.domain.valueobject;

import java.util.Locale;
import java.util.Objects;

public record NormalizedQuery(String value) {
    public NormalizedQuery(String value) {
        String requiredValue = Objects.requireNonNull(value, "value");
        if (requiredValue.isEmpty() || requiredValue.length() > 128) {
            throw new IllegalArgumentException("query must contain between 1 and 128 characters");
        }
        this.value = requiredValue;
    }

    public static NormalizedQuery normalize(String raw) {
        Objects.requireNonNull(raw, "raw");
        return new NormalizedQuery(raw.trim().toUpperCase(Locale.ROOT));
    }
}
