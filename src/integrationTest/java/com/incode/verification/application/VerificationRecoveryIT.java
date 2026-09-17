package com.incode.verification.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.repository.ExpirationLock;
import com.incode.verification.service.ExpirationService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VerificationRecoveryIT {
  @Test
  void startupRecoveryDrainsFullBatches() {
    var calls = new int[] {0};
    var expiration = mock(ExpirationService.class);
    when(expiration.expire(any(Instant.class), eq(100)))
        .thenAnswer(invocation -> ++calls[0] < 3 ? 100 : 4);
    new VerificationExpirationScheduler(
            expiration,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            () ->
                new ExpirationLock.Lease() {
                  @Override
                  public boolean acquired() {
                    return true;
                  }

                  @Override
                  public void close() {}
                })
        .recoverExpiredVerifications();
    assertEquals(3, calls[0]);
  }
}
