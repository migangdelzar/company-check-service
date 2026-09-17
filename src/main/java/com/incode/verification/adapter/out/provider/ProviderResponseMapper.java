package com.incode.verification.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.incode.verification.domain.company.Company;
import com.incode.verification.domain.provider.ProviderType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

final class ProviderResponseMapper {
  private ProviderResponseMapper() {}

  static List<Company> mapCompanies(@Nullable JsonNode root, ProviderType type) {
    if (root == null) {
      throw new IllegalArgumentException("provider payload is empty");
    }
    JsonNode values = root.isArray() ? root : root.path("results");
    if (!values.isArray()) {
      values = root.path("companies");
    }
    if (!values.isArray()) {
      throw new IllegalArgumentException("provider payload is not an array");
    }
    List<Company> result = new ArrayList<>();
    for (JsonNode item : values) {
      String cin =
          type == ProviderType.FREE ? text(item, "cin") : text(item, "companyIdentificationNumber");
      String name = type == ProviderType.FREE ? text(item, "name") : text(item, "companyName");
      String registrationDate =
          type == ProviderType.FREE
              ? text(item, "registration_date")
              : text(item, "registrationDate");
      String address =
          type == ProviderType.FREE ? text(item, "address") : text(item, "fullAddress");
      String activeField = type == ProviderType.FREE ? "is_active" : "isActive";
      if (cin == null
          || name == null
          || registrationDate == null
          || address == null
          || !item.path(activeField).isBoolean()) {
        throw new IllegalArgumentException("provider company is malformed");
      }
      try {
        result.add(
            new Company(
                cin,
                name,
                LocalDate.parse(registrationDate),
                address,
                item.path(activeField).booleanValue()));
      } catch (RuntimeException exception) {
        throw new IllegalArgumentException("provider company is malformed", exception);
      }
    }
    return result;
  }

  private static @Nullable String text(JsonNode node, String... names) {
    for (String name : names) {
      if (node.hasNonNull(name)) {
        return node.path(name).asText();
      }
    }
    return null;
  }
}
