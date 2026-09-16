package com.incode.verification.application.port.in;

import com.incode.verification.application.port.out.VerificationView;
import java.util.UUID;

public interface GetVerificationUseCase {
  VerificationView get(UUID verificationId);
}
