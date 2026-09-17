package com.incode.verification.exception;

public final class CoordinationUnavailableException extends IntegrationException {
  public CoordinationUnavailableException(String message) {
    super(503, "Coordination unavailable", "COORDINATION_UNAVAILABLE", message);
  }
}
