package com.incode.verification.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.application.exception.CoordinationUnavailableException;
import com.incode.verification.application.exception.ProviderSubmissionException;
import com.incode.verification.application.exception.VerificationConflictException;
import com.incode.verification.application.exception.VerificationNotFoundException;
import com.incode.verification.domain.provider.ProviderFailure;
import org.junit.jupiter.api.Test;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void mapsVerificationConflictToProblemDetails() {
    var response =
        handler.verification(
            new VerificationConflictException("VERIFICATION_ID_REUSE", "conflict"));
    assertEquals(409, response.getStatus());
    assertEquals("VERIFICATION_ID_REUSE", response.getProperties().get("code"));
  }

  @Test
  void mapsProviderClientFailureToBadGateway() {
    var response =
        handler.verification(
            new ProviderSubmissionException(new ProviderFailure.ClientError(429), "provider"));
    assertEquals(502, response.getStatus());
    assertEquals("PROVIDER_CLIENT_ERROR", response.getProperties().get("code"));
  }

  @Test
  void mapsProviderAvailabilityFailureToServiceUnavailable() {
    var response =
        handler.verification(
            new ProviderSubmissionException(new ProviderFailure.Timeout(), "timeout"));
    assertEquals(503, response.getStatus());
    assertEquals("PROVIDERS_UNAVAILABLE", response.getProperties().get("code"));
  }

  @Test
  void mapsMissingVerificationToNotFound() {
    var response = handler.verification(new VerificationNotFoundException("missing"));
    assertEquals(404, response.getStatus());
  }

  @Test
  void mapsCoordinationFailureToServiceUnavailable() {
    var response = handler.verification(new CoordinationUnavailableException("redis"));
    assertEquals(503, response.getStatus());
    assertEquals("COORDINATION_UNAVAILABLE", response.getProperties().get("code"));
  }
}
