package com.incode.verification.application.service;

public final class VerificationNotFoundException extends VerificationException {
  public VerificationNotFoundException(String message) {
    super(404, "Verification not found", null, message);
  }
}
