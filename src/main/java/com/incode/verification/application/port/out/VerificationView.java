package com.incode.verification.application.port.out;

import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VerificationView(UUID id, String rawQuery, String normalizedQuery, Instant startedAt,
                               Instant expiresAt, VerificationStatus status, Company company,
                               List<Company> otherResults, ProviderType provider, ProviderFailure failure) {
    public VerificationView {
        otherResults = otherResults == null ? List.of() : List.copyOf(otherResults);
    }
}
