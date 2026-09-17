package com.incode.verification.controller;

import com.incode.verification.controller.dto.response.VerificationResponse;
import com.incode.verification.mapper.VerificationMapper;
import com.incode.verification.service.VerificationService;
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
  private final VerificationService verification;

  public VerificationController(VerificationService verification) {
    this.verification = verification;
  }

  @GetMapping("/verifications/{verificationId}")
  public ResponseEntity<VerificationResponse> get(@PathVariable UUID verificationId) {
    var view = verification.get(verificationId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(VerificationMapper.map(view));
  }
}
