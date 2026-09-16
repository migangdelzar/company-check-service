package com.incode.verification.application.service;

public final class VerificationNotFoundException extends RuntimeException {
  public VerificationNotFoundException(String message) {
    super(message);
  }
}
