package com.incode.verification.application.port.out;

import com.incode.verification.domain.aggregate.Verification;
import java.util.Optional;
import java.util.UUID;

/** PostgreSQL adapter port. Implementations must make save atomic with their transaction. */
public interface VerificationRepository {
    void insertInProgress(Verification verification);
    void update(Verification verification);
    Optional<Verification> findById(UUID id);
}
