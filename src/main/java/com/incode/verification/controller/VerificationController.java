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
import reactor.core.publisher.Mono;

@RestController
@Validated
public class VerificationController {
  private final VerificationService verification;

  public VerificationController(VerificationService verification) {
    this.verification = verification;
  }

  @GetMapping("/verifications/{verificationId}")
  public Mono<ResponseEntity<VerificationResponse>> get(@PathVariable UUID verificationId) {
    return verification
        .get(verificationId)
        .map(VerificationMapper::map)
        .map(body -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body));
  }
}
