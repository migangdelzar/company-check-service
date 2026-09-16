package com.incode.verification.domain.type;

import com.incode.verification.domain.entity.Company;
import java.util.List;
import java.util.Objects;

public sealed interface ProviderLookupResult permits ProviderLookupResult.Success, ProviderLookupResult.Failure {
    record Success(List<Company> companies) implements ProviderLookupResult {
        public Success { companies = List.copyOf(Objects.requireNonNull(companies, "companies")); }
    }

    record Failure(ProviderFailure failure) implements ProviderLookupResult {
        public Failure { Objects.requireNonNull(failure, "failure"); }
    }
}
