package com.incode.verification.exception.domain;

import com.incode.verification.exception.base.BusinessException;

public final class VerificationConflictException extends BusinessException {
  public VerificationConflictException(String code, String message) {
    super(409, "Verification conflict", code, message);
  }
}
