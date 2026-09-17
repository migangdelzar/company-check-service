package com.incode.verification.exception;

import com.incode.verification.exception.base.BusinessException;
import org.jspecify.annotations.Nullable;

/** Base for failures while coordinating with external infrastructure. */
public abstract class IntegrationException extends BusinessException {
  protected IntegrationException(int status, String title, @Nullable String code, String message) {
    super(status, title, code, message);
  }
}
