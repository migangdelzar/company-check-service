package com.incode.verification.domain.valueobject;

public final class InvalidQueryException extends IllegalArgumentException {
  public InvalidQueryException(String message) {
    super(message);
  }
}
