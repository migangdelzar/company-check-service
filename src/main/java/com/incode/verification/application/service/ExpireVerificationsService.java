package com.incode.verification.application.service;

import com.incode.verification.application.port.in.ExpireVerificationsUseCase;
import com.incode.verification.application.port.out.VerificationRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ExpireVerificationsService implements ExpireVerificationsUseCase {
  private final VerificationRepository repository;

  public ExpireVerificationsService(VerificationRepository repository) {
    this.repository = repository;
  }

  @Override
  @Observed(name = "verification.expire")
  public int expire(Instant now, int batchSize) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
    return repository.expireBatch(now, batchSize);
  }
}
