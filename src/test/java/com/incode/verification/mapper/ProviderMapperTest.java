package com.incode.verification.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.incode.verification.client.dto.FreeCompanyResponse;
import com.incode.verification.client.dto.PremiumCompanyResponse;
import org.junit.jupiter.api.Test;

class ProviderMapperTest {
  @Test
  void mapsFreeProviderResponseToCompany() {
    var result =
        ProviderMapper.mapFree(
            new FreeCompanyResponse[] {
              new FreeCompanyResponse("123", "Acme", "2020-01-01", "1 Main St", true)
            });

    assertEquals("Acme", result.getFirst().name());
    assertEquals("1 Main St", result.getFirst().address());
    assertEquals(true, result.getFirst().isActive());
  }

  @Test
  void mapsPremiumProviderResponseToCompany() {
    var result =
        ProviderMapper.mapPremium(
            new PremiumCompanyResponse[] {
              new PremiumCompanyResponse("123", "Acme", "2020-01-01", "1 Main St", true)
            });

    assertEquals("Acme", result.getFirst().name());
    assertEquals("1 Main St", result.getFirst().address());
    assertEquals(true, result.getFirst().isActive());
  }

  @Test
  void mapsAnEmptyProviderArrayToNoMatches() {
    assertEquals(0, ProviderMapper.mapFree(new FreeCompanyResponse[0]).size());
    assertEquals(0, ProviderMapper.mapPremium(new PremiumCompanyResponse[0]).size());
  }

  @Test
  void rejectsNullProviderPayload() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ProviderMapper.mapPremium((PremiumCompanyResponse[]) null));
  }
}
