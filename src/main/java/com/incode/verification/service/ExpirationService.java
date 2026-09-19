package com.incode.verification.service;

import com.incode.verification.repository.VerificationRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.Instant;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ExpirationService {
  private final VerificationRepository repository;

  public ExpirationService(VerificationRepository repository) {
    this.repository = repository;
  }

  @Observed(name = "verification.expire")
  public Mono<Integer> expire(Instant now, int batchSize) {
    if (batchSize < 1) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
    return repository.expireBatch(now, batchSize);
  }
}
