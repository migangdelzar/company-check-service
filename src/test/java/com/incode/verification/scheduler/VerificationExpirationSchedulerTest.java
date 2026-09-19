package com.incode.verification.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.incode.verification.repository.ExpirationLock;
import com.incode.verification.service.ExpirationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class VerificationExpirationSchedulerTest {
  @Test
  void skipsExpirationWhenAnotherInstanceOwnsTheLease() {
    var expirationService = mock(ExpirationService.class);
    ExpirationLock lock = () -> Mono.just(new TestLease(false));
    var scheduler =
        new VerificationExpirationScheduler(
            expirationService, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), lock);

    scheduler.reapExpiredVerifications().block();

    verifyNoInteractions(expirationService);
  }

  @Test
  void drainsFullBatchesAndReleasesTheLease() {
    var expirationService = mock(ExpirationService.class);
    when(expirationService.expire(any(Instant.class), eq(100)))
        .thenReturn(Mono.just(100), Mono.just(4));
    var released = new java.util.concurrent.atomic.AtomicBoolean();
    ExpirationLock lock =
        () ->
            Mono.just(
                new ExpirationLock.Lease() {
                  @Override
                  public boolean acquired() {
                    return true;
                  }

                  @Override
                  public Mono<Void> release() {
                    released.set(true);
                    return Mono.empty();
                  }
                });
    var scheduler =
        new VerificationExpirationScheduler(
            expirationService, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), lock);

    scheduler.reapExpiredVerifications().block();

    verify(expirationService, org.mockito.Mockito.times(2)).expire(any(Instant.class), eq(100));
    org.junit.jupiter.api.Assertions.assertTrue(released.get());
  }

  private record TestLease(boolean acquired) implements ExpirationLock.Lease {
    @Override
    public Mono<Void> release() {
      return Mono.empty();
    }
  }
}
