package com.incode.verification.exception.base;

import org.jspecify.annotations.Nullable;

public abstract class BusinessException extends RuntimeException {
  private final int status;
  private final String title;
  private final @Nullable String code;

  protected BusinessException(int status, String title, @Nullable String code, String message) {
    super(message);
    this.status = status;
    this.title = title;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String title() {
    return title;
  }

  public @Nullable String code() {
    return code;
  }
}
