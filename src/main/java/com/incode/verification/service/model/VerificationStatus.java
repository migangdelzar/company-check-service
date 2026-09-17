package com.incode.verification.service.model;

public enum VerificationStatus {
  IN_PROGRESS,
  COMPLETED,
  FAILED;

  public boolean isTerminal() {
    return this != IN_PROGRESS;
  }
}
