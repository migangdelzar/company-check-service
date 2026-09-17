package com.incode.verification.adapter.in.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.application.port.in.ExpireVerificationsUseCase;
import com.incode.verification.application.port.out.ExpirationLock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VerificationExpirationSchedulerTest {
  @Test
  void skipsExpirationWhenAnotherInstanceOwnsTheLease() {
    var calls = new int[] {0};
    ExpireVerificationsUseCase useCase = (now, batch) -> ++calls[0];
    ExpirationLock lock = () -> new TestLease(false);
    var scheduler =
        new VerificationExpirationScheduler(
            useCase, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), lock);

    scheduler.reapExpiredVerifications();

    assertEquals(0, calls[0]);
  }

  private record TestLease(boolean acquired) implements ExpirationLock.Lease {
    @Override
    public void close() {}
  }
}
