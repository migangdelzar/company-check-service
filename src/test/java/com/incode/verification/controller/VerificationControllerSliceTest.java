package com.incode.verification.controller;

import static org.mockito.Mockito.when;

import com.incode.verification.repository.InboundRateLimiter;
import com.incode.verification.service.VerificationService;
import com.incode.verification.service.model.VerificationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(VerificationController.class)
class VerificationControllerSliceTest {
  @Autowired private WebTestClient client;

  @MockitoBean private VerificationService getVerification;

  @MockitoBean private InboundRateLimiter inboundRateLimiter;

  @Test
  void returnsNoStoreForVerificationReads() {
    var id = UUID.randomUUID();
    when(getVerification.get(id))
        .thenReturn(
            Mono.just(
                new VerificationResult(
                    id, "Acme", "acme", null, null, null, null, null, null, null)));

    client
        .get()
        .uri("/verifications/{verificationId}", id)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals("Cache-Control", "no-store");
  }
}
