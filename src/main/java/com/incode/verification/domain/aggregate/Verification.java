package com.incode.verification.domain.aggregate;

import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.*;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.Instant;
import java.util.*;

public record Verification(UUID id, String rawQuery, NormalizedQuery query, Instant startedAt, Instant expiresAt,
                           VerificationState state) {
    public Verification {
        Objects.requireNonNull(id); Objects.requireNonNull(rawQuery); Objects.requireNonNull(query);
        Objects.requireNonNull(startedAt); Objects.requireNonNull(expiresAt); Objects.requireNonNull(state);
        if (!expiresAt.isAfter(startedAt)) throw new IllegalArgumentException("expiresAt must be after startedAt");
    }

    public static Verification start(UUID id, String rawQuery, NormalizedQuery normalized, Instant now, Instant expiresAt) {
        return new Verification(id, rawQuery, normalized, now, expiresAt, new VerificationState.InProgress());
    }

    public Verification complete(ProviderLookupResult.Success result, Instant now) {
        requireInProgress();
        List<Company> active = result.companies().stream().filter(Company::active).toList();
        if (active.isEmpty()) return fail(new ProviderFailure.Unavailable(), now);
        return new Verification(id, rawQuery, query, startedAt, expiresAt,
                new VerificationState.Completed(active.getFirst(), active.subList(1, active.size()), result.provider()));
    }

    public Verification fail(ProviderFailure failure, Instant now) {
        requireInProgress();
        Objects.requireNonNull(now);
        return new Verification(id, rawQuery, query, startedAt, expiresAt, new VerificationState.Failed(failure));
    }

    private void requireInProgress() {
        if (!(state instanceof VerificationState.InProgress)) throw new IllegalStateException("verification is terminal");
    }
}
