package com.incode.verification.application.port.in;

import com.incode.verification.application.port.out.VerificationView;
import java.util.UUID;

public interface StartVerificationUseCase {
  VerificationView start(StartVerificationCommand command);

  record StartVerificationCommand(UUID verificationId, String query) {}
}
