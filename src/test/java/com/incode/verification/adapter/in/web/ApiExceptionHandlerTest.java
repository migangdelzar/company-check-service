package com.incode.verification.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.application.service.ProviderSubmissionException;
import com.incode.verification.application.service.VerificationConflictException;
import com.incode.verification.application.service.VerificationNotFoundException;
import com.incode.verification.domain.type.ProviderFailure;
import org.junit.jupiter.api.Test;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapsVerificationConflictToProblemDetails() {
    var response =
        handler.conflict(new VerificationConflictException("VERIFICATION_ID_REUSE", "conflict"));
    assertEquals(409, response.getStatusCode().value());
    assertEquals("VERIFICATION_ID_REUSE", response.getBody().getProperties().get("code"));
  }

  @Test
  void mapsProviderClientFailureToBadGateway() {
    var response =
        handler.provider(
            new ProviderSubmissionException(new ProviderFailure.ClientError(429), "provider"));
    assertEquals(502, response.getStatusCode().value());
    assertEquals("PROVIDER_CLIENT_ERROR", response.getBody().getProperties().get("code"));
  }

  @Test
  void mapsProviderAvailabilityFailureToServiceUnavailable() {
    var response =
        handler.provider(new ProviderSubmissionException(new ProviderFailure.Timeout(), "timeout"));
    assertEquals(503, response.getStatusCode().value());
    assertEquals("PROVIDERS_UNAVAILABLE", response.getBody().getProperties().get("code"));
  }

  @Test
  void mapsMissingVerificationToNotFound() {
    var response = handler.notFound(new VerificationNotFoundException("missing"));
    assertEquals(404, response.getStatusCode().value());
  }
}
