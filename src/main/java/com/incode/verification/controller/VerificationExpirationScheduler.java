package com.incode.verification.controller;

import com.incode.verification.repository.ExpirationLock;
import com.incode.verification.service.ExpirationService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public final class VerificationExpirationScheduler {
  private static final int BATCH_SIZE = 100;
  private final ExpirationService expiration;
  private final Clock clock;
  private final ExpirationLock expirationLock;

  public VerificationExpirationScheduler(
      ExpirationService expiration, Clock clock, ExpirationLock expirationLock) {
    this.expiration = expiration;
    this.clock = clock;
    this.expirationLock = expirationLock;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void recoverExpiredVerifications() {
    expireUntilDrained();
  }

  @Scheduled(fixedDelayString = "${verification.expiration.reaper-delay:1000ms}")
  public void reapExpiredVerifications() {
    expireUntilDrained();
  }

  private void expireUntilDrained() {
    int expired;
    do {
      try (var lease = expirationLock.tryAcquire()) {
        if (!lease.acquired()) {
          return;
        }
        expired = expiration.expire(Instant.now(clock), BATCH_SIZE);
      }
    } while (expired == BATCH_SIZE);
  }
}
