package com.incode.verification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.incode.verification.controller.dto.response.VerificationResponse;
import com.incode.verification.service.model.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class VerificationResponseJsonSliceTest {
  @Autowired private JacksonTester<VerificationResponse> json;

  @Test
  void serializesVerificationResponseWithStableApiFields() throws Exception {
    var response =
        VerificationResponse.builder()
            .verificationId(UUID.fromString("018f0f5d-7b3a-7c6e-8e2d-123456789abc"))
            .query("Acme")
            .normalizedQuery("acme")
            .startedAt(Instant.parse("2026-01-01T00:00:00Z"))
            .expiresAt(Instant.parse("2026-01-01T00:10:00Z"))
            .status(VerificationStatus.COMPLETED)
            .otherResults(List.of())
            .build();

    assertThat(json.write(response))
        .extractingJsonPathStringValue("$.verificationId")
        .isEqualTo("018f0f5d-7b3a-7c6e-8e2d-123456789abc");
    assertThat(json.write(response))
        .extractingJsonPathStringValue("$.status")
        .isEqualTo("COMPLETED");
  }
}
