package com.incode.verification.mapper;

import com.incode.verification.client.dto.FreeCompanyResponse;
import com.incode.verification.client.dto.PremiumCompanyResponse;
import com.incode.verification.service.model.Company;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class ProviderMapper {
  private ProviderMapper() {}

  public static List<Company> mapFree(FreeCompanyResponse @Nullable [] payload) {
    return map(payload, FreeCompanyResponse::toCompany);
  }

  public static List<Company> mapPremium(PremiumCompanyResponse @Nullable [] payload) {
    return map(payload, PremiumCompanyResponse::toCompany);
  }

  private static <T> List<Company> map(T @Nullable [] payload, Function<T, Company> companyMapper) {
    if (payload == null) {
      throw new IllegalArgumentException("provider payload is empty");
    }
    List<Company> companies = new ArrayList<>(payload.length);
    for (T item : payload) {
      if (item == null) {
        throw new IllegalArgumentException("provider company is malformed");
      }
      companies.add(companyMapper.apply(item));
    }
    return List.copyOf(companies);
  }
}
