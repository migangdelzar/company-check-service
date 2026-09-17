package com.incode.verification.application.service;

public final class VerificationConflictException extends VerificationException {
  public VerificationConflictException(String code, String message) {
    super(409, "Verification conflict", code, message);
  }
}
