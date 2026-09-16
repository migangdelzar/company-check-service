package com.incode.verification.application.port.out;

public final class VerificationAlreadyExistsException extends RuntimeException {
  public VerificationAlreadyExistsException(Throwable cause) {
    super(cause);
  }
}
