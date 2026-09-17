package com.incode.verification.domain.verification;

public enum VerificationStatus {
  IN_PROGRESS,
  COMPLETED,
  FAILED;

  public boolean isTerminal() {
    return this != IN_PROGRESS;
  }
}
