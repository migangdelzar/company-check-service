package com.incode.verification.exception.domain;

import com.incode.verification.exception.base.BusinessException;

public final class VerificationNotFoundException extends BusinessException {
  public VerificationNotFoundException(String message) {
    super(404, "Verification not found", null, message);
  }
}
