package com.incode.verification.domain.query;

public final class InvalidQueryException extends IllegalArgumentException {
  public InvalidQueryException(String message) {
    super(message);
  }
}
