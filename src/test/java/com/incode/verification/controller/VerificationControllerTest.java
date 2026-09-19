package com.incode.verification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incode.verification.exception.handler.GlobalExceptionHandler;
import com.incode.verification.service.VerificationService;
import com.incode.verification.service.model.VerificationResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class VerificationControllerTest {
  private final VerificationService retriever = mock(VerificationService.class);
  private final WebTestClient client =
      WebTestClient.bindToController(new VerificationController(retriever))
          .controllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void retrievalDelegatesByVerificationId() {
    var id = UUID.randomUUID();
    when(retriever.get(id)).thenReturn(Mono.just(view(id)));

    client
        .get()
        .uri("/verifications/{id}", id)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.verificationId")
        .isEqualTo(id.toString());
    verify(retriever).get(id);
  }

  private VerificationResult view(UUID id) {
    var now = Instant.parse("2026-01-01T00:00:00Z");
    return new VerificationResult(
        id,
        "Acme",
        "acme",
        now,
        now.plusSeconds(600),
        com.incode.verification.service.model.VerificationStatus.IN_PROGRESS,
        null,
        null,
        null,
        null);
  }
}
