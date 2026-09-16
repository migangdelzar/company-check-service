package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.out.VerificationView;
import com.incode.verification.domain.entity.Company;
import com.incode.verification.domain.type.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VerificationResponse(UUID verificationId, String query, String normalizedQuery,
        Instant startedAt, Instant expiresAt, VerificationStatus status, CompanyResponse company,
        List<CompanyResponse> otherResults, String provider, String failure) {
    public VerificationResponse {
        otherResults = otherResults == null ? List.of() : List.copyOf(otherResults);
    }

    public static VerificationResponse from(VerificationView view) {
        return new VerificationResponse(view.id(), view.rawQuery(), view.normalizedQuery(), view.startedAt(),
                view.expiresAt(), view.status(), CompanyResponse.from(view.company()),
                view.otherResults().stream().map(CompanyResponse::from).toList(),
                view.provider() == null ? null : view.provider().name(),
                view.failure() == null ? null : view.failure().getClass().getSimpleName());
    }

    public record CompanyResponse(String name, String address, boolean active) {
        static CompanyResponse from(Company company) {
            return company == null ? null : new CompanyResponse(company.name(), company.address(), company.active());
        }
    }
}
