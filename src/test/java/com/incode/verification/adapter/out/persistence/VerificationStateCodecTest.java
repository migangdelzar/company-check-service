package com.incode.verification.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.domain.provider.ProviderFailure;
import com.incode.verification.domain.verification.VerificationState;
import org.junit.jupiter.api.Test;

class VerificationStateCodecTest {
  @Test
  void preservesClientErrorStatusCodeDuringRoundTrip() {
    var codec = new VerificationStateCodec(new ObjectMapper().findAndRegisterModules());
    var decoded =
        codec.decode(
            codec.encode(new VerificationState.Failed(new ProviderFailure.ClientError(429))));
    var failure =
        assertInstanceOf(
            ProviderFailure.ClientError.class,
            assertInstanceOf(VerificationState.Failed.class, decoded).failure());
    assertEquals(429, failure.statusCode());
  }
}
