package com.incode.verification.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.VerificationState;
import org.junit.jupiter.api.Test;

class VerificationStateMapperTest {
  @Test
  void preservesClientErrorStatusCodeDuringRoundTrip() {
    var mapper = new VerificationStateMapper(new ObjectMapper().findAndRegisterModules());
    var decoded =
        mapper.decode(
            mapper.encode(new VerificationState.Failed(new ProviderFailure.ClientError(429))));
    var failure =
        assertInstanceOf(
            ProviderFailure.ClientError.class,
            assertInstanceOf(VerificationState.Failed.class, decoded).failure());
    assertEquals(429, failure.statusCode());
  }
}
