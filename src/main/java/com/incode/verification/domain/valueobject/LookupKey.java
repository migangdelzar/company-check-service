package com.incode.verification.domain.valueobject;

import java.util.Objects;

public record LookupKey(NormalizedQuery query) {
    public LookupKey {
        Objects.requireNonNull(query, "query");
    }
}
