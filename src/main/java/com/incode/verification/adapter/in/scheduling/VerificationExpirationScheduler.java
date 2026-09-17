package com.incode.verification.adapter.in.scheduling;

import com.incode.verification.application.port.in.ExpireVerificationsUseCase;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public final class VerificationExpirationScheduler {
  private static final int BATCH_SIZE = 100;
  private final ExpireVerificationsUseCase expiration;
  private final Clock clock;

  public VerificationExpirationScheduler(ExpireVerificationsUseCase expiration, Clock clock) {
    this.expiration = expiration;
    this.clock = clock;
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
      expired = expiration.expire(Instant.now(clock), BATCH_SIZE);
    } while (expired == BATCH_SIZE);
  }
}
