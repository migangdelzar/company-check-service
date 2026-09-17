package com.incode.verification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.incode.verification.repository.ExpirationLock;
import com.incode.verification.service.ExpirationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VerificationExpirationSchedulerTest {
  @Test
  void skipsExpirationWhenAnotherInstanceOwnsTheLease() {
    var expirationService = mock(ExpirationService.class);
    ExpirationLock lock = () -> new TestLease(false);
    var scheduler =
        new VerificationExpirationScheduler(
            expirationService, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), lock);

    scheduler.reapExpiredVerifications();

    verifyNoInteractions(expirationService);
  }

  private record TestLease(boolean acquired) implements ExpirationLock.Lease {
    @Override
    public void close() {}
  }
}
