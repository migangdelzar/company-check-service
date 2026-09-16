package com.incode.verification.adapter.out.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.verification.domain.type.ProviderType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProviderResponseMapperTest {
  @Test
  void mapsPremiumFullAddress() throws Exception {
    var json =
        new ObjectMapper()
            .readTree(
                "{\"results\":[{\"companyIdentificationNumber\":\"123\","
                    + "\"companyName\":\"Acme\",\"registrationDate\":\"2020-01-01\","
                    + "\"fullAddress\":\"1 Main St\",\"isActive\":true}]}");
    var result = ProviderResponseMapper.companies(json, ProviderType.PREMIUM);
    assertEquals("Acme", result.getFirst().name());
    assertEquals("1 Main St", result.getFirst().address());
    assertEquals("123", result.getFirst().cin());
    assertEquals(LocalDate.parse("2020-01-01"), result.getFirst().registrationDate());
  }

  @Test
  void mapsFreeProviderFieldsAndActiveFlag() throws Exception {
    var json =
        new ObjectMapper()
            .readTree(
                "[{\"cin\":\"123\",\"name\":\"Acme\","
                    + "\"registration_date\":\"2020-01-01\",\"address\":\"1 Main St\","
                    + "\"is_active\":false}]");
    var result = ProviderResponseMapper.companies(json, ProviderType.FREE);
    assertEquals("Acme", result.getFirst().name());
    assertEquals("1 Main St", result.getFirst().address());
    assertEquals(false, result.getFirst().isActive());
  }

  @Test
  void mapsPremiumProviderFieldsAndEmptyPayloadAsNoMatch() throws Exception {
    var json =
        new ObjectMapper()
            .readTree(
                "[{\"companyIdentificationNumber\":\"123\",\"companyName\":\"Acme\","
                    + "\"registrationDate\":\"2020-01-01\",\"fullAddress\":\"1 Main St\","
                    + "\"isActive\":true}]");
    var result = ProviderResponseMapper.companies(json, ProviderType.PREMIUM);
    assertEquals("Acme", result.getFirst().name());
    assertEquals("1 Main St", result.getFirst().address());
    assertEquals(true, result.getFirst().isActive());
    assertEquals(
        0,
        ProviderResponseMapper.companies(new ObjectMapper().readTree("[]"), ProviderType.FREE)
            .size());
  }

  @Test
  void rejectsTheStalePremiumAddressField() throws Exception {
    var json =
        new ObjectMapper()
            .readTree(
                "[{\"companyIdentificationNumber\":\"123\",\"companyName\":\"Acme\","
                    + "\"registrationDate\":\"2020-01-01\","
                    + "\"companyFullAddress\":\"1 Main St\",\"isActive\":true}]");

    assertThrows(
        IllegalArgumentException.class,
        () -> ProviderResponseMapper.companies(json, ProviderType.PREMIUM));
  }
}
