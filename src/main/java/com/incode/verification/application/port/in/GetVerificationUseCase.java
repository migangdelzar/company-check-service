package com.incode.verification.application.port.in;

import com.incode.verification.application.result.VerificationResult;
import java.util.UUID;

public interface GetVerificationUseCase {
  VerificationResult get(UUID verificationId);
}
