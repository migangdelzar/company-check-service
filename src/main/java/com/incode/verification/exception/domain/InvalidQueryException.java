package com.incode.verification.exception.domain;

public final class InvalidQueryException extends IllegalArgumentException {
  public InvalidQueryException(String message) {
    super(message);
  }
}
