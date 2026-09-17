package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.in.StartVerificationCommand;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class BackendServiceController {
  private final StartVerificationUseCase startVerification;

  public BackendServiceController(StartVerificationUseCase startVerification) {
    this.startVerification = startVerification;
  }

  @GetMapping("/backend-service")
  public ResponseEntity<VerificationResponse> start(
      @Valid @ModelAttribute BackendServiceRequest request) {
    var view =
        startVerification.start(
            new StartVerificationCommand(request.verificationId(), request.query()));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(VerificationResponse.from(view));
  }
}
