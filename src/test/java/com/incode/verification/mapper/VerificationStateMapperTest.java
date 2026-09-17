package com.incode.verification.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.config.ApplicationConfiguration;
import com.incode.verification.service.model.Company;
import com.incode.verification.service.model.ProviderFailure;
import com.incode.verification.service.model.ProviderType;
import com.incode.verification.service.model.VerificationState;
import java.time.LocalDate;
import java.util.List;
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

  @Test
  void preservesLocalDateWhenEncodingCompletedState() {
    var company = new Company("CIN123", "Example", LocalDate.of(2020, 1, 2), "Address", true);
    var state = new VerificationState.Completed(company, List.of(), ProviderType.FREE);
    var mapper = new VerificationStateMapper(new ApplicationConfiguration().objectMapper());

    var decoded =
        assertInstanceOf(VerificationState.Completed.class, mapper.decode(mapper.encode(state)));

    assertEquals(company, decoded.company());
  }
}
