package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.in.GetVerificationUseCase;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class VerificationController {
  private final GetVerificationUseCase getVerification;

  public VerificationController(GetVerificationUseCase getVerification) {
    this.getVerification = getVerification;
  }

  @GetMapping("/verifications/{verificationId}")
  public ResponseEntity<VerificationResponse> get(@PathVariable UUID verificationId) {
    var view = getVerification.get(verificationId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(VerificationResponse.from(view));
  }
}
