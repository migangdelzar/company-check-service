package com.incode.verification.application.service;

public final class VerificationConflictException extends RuntimeException {
  private final String code;

  public VerificationConflictException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
