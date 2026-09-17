package com.incode.verification.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.adapter.in.scheduling.VerificationExpirationScheduler;
import com.incode.verification.application.port.in.ExpireVerificationsUseCase;
import com.incode.verification.application.port.out.ExpirationLock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VerificationRecoveryIT {
  @Test
  void startupRecoveryDrainsFullBatches() {
    var calls = new int[] {0};
    var useCase = (ExpireVerificationsUseCase) (now, batch) -> ++calls[0] < 3 ? batch : 4;
    new VerificationExpirationScheduler(
            useCase,
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
