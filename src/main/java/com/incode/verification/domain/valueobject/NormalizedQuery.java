package com.incode.verification.domain.valueobject;

import java.util.Locale;
import java.util.Objects;

public record NormalizedQuery(String value) {
    public NormalizedQuery {
        value = Objects.requireNonNull(value, "value");
        if (value.isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("query must contain between 1 and 128 characters");
        }
    }

    public static NormalizedQuery normalize(String raw) {
        Objects.requireNonNull(raw, "raw");
        return new NormalizedQuery(raw.trim().toUpperCase(Locale.ROOT));
    }
}
