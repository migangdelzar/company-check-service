package com.incode.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.repository.VerificationRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class ExpirationServiceTest {
  private final VerificationRepository repository = mock(VerificationRepository.class);
  private final ExpirationService service = new ExpirationService(repository);

  @Test
  void expiresTheRequestedBatch() {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    when(repository.expireBatch(now, 50)).thenReturn(Mono.just(3));

    assertEquals(3, service.expire(now, 50).block());
    verify(repository).expireBatch(now, 50);
  }

  @Test
  void rejectsNonPositiveBatchSizes() {
    assertThrows(IllegalArgumentException.class, () -> service.expire(Instant.EPOCH, 0));
  }
}
