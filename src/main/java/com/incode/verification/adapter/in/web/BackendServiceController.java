package com.incode.verification.adapter.in.web;

import com.incode.verification.application.port.in.GetVerificationUseCase;
import com.incode.verification.application.port.in.StartVerificationUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import com.incode.verification.adapter.out.observability.MicrometerTelemetryAdapter;
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
  private final MicrometerTelemetryAdapter telemetry;

  public BackendServiceController(
      StartVerificationUseCase starter, GetVerificationUseCase retriever) {
    this(starter, retriever, null);
  }

  public BackendServiceController(
      StartVerificationUseCase starter,
      GetVerificationUseCase retriever,
      MicrometerTelemetryAdapter telemetry) {
    this.starter = starter;
    this.retriever = retriever;
    this.telemetry = telemetry;
  }

  @PostMapping("/backend-service")
  public ResponseEntity<VerificationResponse> lookup(
      @Valid @ModelAttribute BackendServiceRequest request) {
    var started = Instant.now();
    var result =
        starter.start(
            new StartVerificationUseCase.StartVerificationCommand(
                request.verificationId(), request.query()));
    if (telemetry != null) {
      telemetry.operation("start", result.status().name());
      telemetry.latency("start", Duration.between(started, Instant.now()));
    }
    return response(result);
  }

  @GetMapping("/verifications/{verificationId}")
  public ResponseEntity<VerificationResponse> retrieve(@PathVariable UUID verificationId) {
    var started = Instant.now();
    var result = retriever.get(verificationId);
    if (telemetry != null) {
      telemetry.operation("get", result.status().name());
      telemetry.latency("get", Duration.between(started, Instant.now()));
    }
    return response(result);
  }

  private ResponseEntity<VerificationResponse> response(
      com.incode.verification.application.port.out.VerificationView view) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(VerificationResponse.from(view));
  }
}
