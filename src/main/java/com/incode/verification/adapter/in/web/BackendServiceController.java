package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Validated
public class BackendServiceController {
  private final StartVerificationUseCase starter;
  private final GetVerificationUseCase retriever;

  public BackendServiceController(
      StartVerificationUseCase starter, GetVerificationUseCase retriever) {
    this.starter = starter;
    this.retriever = retriever;
  }

  @PostMapping("/backend-service")
  public ResponseEntity<VerificationResponse> lookup(
      @Valid @ModelAttribute BackendServiceRequest request) {
    var result =
        starter.start(
            new StartVerificationUseCase.StartVerificationCommand(
                request.verificationId(), request.query()));
    return response(result);
  }

  @GetMapping("/verifications/{verificationId}")
  public ResponseEntity<VerificationResponse> retrieve(@PathVariable UUID verificationId) {
    return response(retriever.get(verificationId));
  }

  private ResponseEntity<VerificationResponse> response(
      com.incode.verification.application.port.out.VerificationView view) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(VerificationResponse.from(view));
  }
}
