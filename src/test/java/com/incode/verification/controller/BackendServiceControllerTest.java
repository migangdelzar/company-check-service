package com.incode.verification.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.incode.verification.exception.handler.GlobalExceptionHandler;
import com.incode.verification.service.VerificationService;
import com.incode.verification.service.model.VerificationResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

class BackendServiceControllerTest {
  private final VerificationService starter = mock(VerificationService.class);
  private final WebTestClient client =
      WebTestClient.bindToController(new BackendServiceController(starter))
          .controllerAdvice(new GlobalExceptionHandler())
          .build();

  @Test
  void lookupReturnsPdfCompatibleRepresentationAndNoStore() {
    var id = UUID.randomUUID();
    when(starter.start(any())).thenReturn(Mono.just(view(id)));

    client
        .get()
        .uri(
            uri ->
                uri.path("/backend-service")
                    .queryParam("verificationId", id)
                    .queryParam("query", "Acme")
                    .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals("Cache-Control", "no-store")
        .expectBody()
        .jsonPath("$.verificationId")
        .isEqualTo(id.toString())
        .jsonPath("$.query")
        .isEqualTo("Acme");
  }

  @Test
  void blankQueryUsesProblemDetails() {
    client
        .get()
        .uri(
            uri ->
                uri.path("/backend-service")
                    .queryParam("verificationId", UUID.randomUUID())
                    .queryParam("query", " ")
                    .build())
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentTypeCompatibleWith("application/problem+json")
        .expectBody()
        .jsonPath("$.title")
        .isEqualTo("Invalid request");
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
