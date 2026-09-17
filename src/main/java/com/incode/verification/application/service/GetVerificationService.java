package com.incode.verification.application.service;

import com.incode.verification.application.exception.VerificationNotFoundException;
import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.out.VerificationRepository;
import com.incode.verification.application.result.VerificationResult;
import com.incode.verification.domain.verification.Verification;
import com.incode.verification.domain.verification.VerificationState;
import io.micrometer.observation.annotation.Observed;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetVerificationService implements GetVerificationUseCase {
  private final VerificationRepository verificationRepository;
  private final VerificationRecoveryService verificationRecovery;

  public GetVerificationService(
      VerificationRepository verificationRepository,
      VerificationRecoveryService verificationRecovery) {
    this.verificationRepository = verificationRepository;
    this.verificationRecovery = verificationRecovery;
  }

  @Override
  @Observed(name = "verification.get")
  public VerificationResult get(UUID verificationId) {
    return verificationRepository
        .findById(verificationId)
        .map(this::resolve)
        .orElseThrow(
            () -> new VerificationNotFoundException("verification not found: " + verificationId));
  }

  private VerificationResult resolve(Verification verification) {
    if (!(verification.state() instanceof VerificationState.InProgress)) {
      return VerificationResult.from(verification);
    }
    return verificationRecovery
        .recover(verification)
        .orElseGet(() -> VerificationResult.from(verification));
  }
}
