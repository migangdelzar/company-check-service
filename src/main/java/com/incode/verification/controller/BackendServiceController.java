package com.incode.verification.controller;

import com.incode.verification.controller.dto.request.BackendServiceRequest;
import com.incode.verification.controller.dto.response.VerificationResponse;
import com.incode.verification.mapper.VerificationMapper;
import com.incode.verification.service.VerificationService;
import com.incode.verification.service.model.StartVerificationCommand;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
public class BackendServiceController {
  private final VerificationService verification;

  public BackendServiceController(VerificationService verification) {
    this.verification = verification;
  }

  @GetMapping("/backend-service")
  public Mono<ResponseEntity<VerificationResponse>> start(
      @Valid @ModelAttribute BackendServiceRequest request) {
    return verification
        .start(new StartVerificationCommand(request.verificationId(), request.query()))
        .map(VerificationMapper::map)
        .map(body -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body));
  }
}
