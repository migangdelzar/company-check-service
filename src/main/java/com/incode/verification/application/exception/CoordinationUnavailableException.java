package com.incode.verification.application.exception;

public final class CoordinationUnavailableException extends VerificationException {
  public CoordinationUnavailableException(String message) {
    super(503, "Coordination unavailable", "COORDINATION_UNAVAILABLE", message);
  }
}
