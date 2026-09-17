package com.incode.verification.exception.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.incode.verification.exception.CoordinationUnavailableException;
import com.incode.verification.exception.ProviderSubmissionException;
import com.incode.verification.exception.domain.VerificationConflictException;
import com.incode.verification.exception.domain.VerificationNotFoundException;
import com.incode.verification.service.model.ProviderFailure;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsVerificationConflictToProblemDetails() {
    var response =
        handler.business(new VerificationConflictException("VERIFICATION_ID_REUSE", "conflict"));
    assertEquals(409, response.getStatus());
    assertEquals("VERIFICATION_ID_REUSE", response.getProperties().get("code"));
  }

  @Test
  void mapsProviderClientFailureToBadGateway() {
    var response =
        handler.business(
            new ProviderSubmissionException(new ProviderFailure.ClientError(429), "provider"));
    assertEquals(502, response.getStatus());
    assertEquals("PROVIDER_CLIENT_ERROR", response.getProperties().get("code"));
  }

  @Test
  void mapsProviderAvailabilityFailureToServiceUnavailable() {
    var response =
        handler.business(new ProviderSubmissionException(new ProviderFailure.Timeout(), "timeout"));
    assertEquals(503, response.getStatus());
    assertEquals("PROVIDERS_UNAVAILABLE", response.getProperties().get("code"));
  }

  @Test
  void mapsMissingVerificationToNotFound() {
    var response = handler.business(new VerificationNotFoundException("missing"));
    assertEquals(404, response.getStatus());
  }

  @Test
  void mapsCoordinationFailureToServiceUnavailable() {
    var response = handler.business(new CoordinationUnavailableException("redis"));
    assertEquals(503, response.getStatus());
    assertEquals("COORDINATION_UNAVAILABLE", response.getProperties().get("code"));
  }
}
