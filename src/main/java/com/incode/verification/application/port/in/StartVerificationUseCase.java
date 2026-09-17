package com.incode.verification.application.port.in;

import com.incode.verification.application.result.VerificationResult;

public interface StartVerificationUseCase {
  VerificationResult start(StartVerificationCommand command);
}
