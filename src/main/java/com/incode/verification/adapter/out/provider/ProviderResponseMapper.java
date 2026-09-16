package com.incode.verification.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.incode.verification.domain.entity.Company;
import java.util.ArrayList;
import java.util.List;

final class ProviderResponseMapper {
    private ProviderResponseMapper() { }
    static List<Company> companies(JsonNode root) {
        JsonNode values = root.isArray() ? root : root.path("companies");
        if (!values.isArray()) values = root.path("results");
        List<Company> result = new ArrayList<>();
        if (values.isArray()) for (JsonNode item : values) {
            String name = text(item, "name", "companyName");
            String address = text(item, "address", "fullAddress", "companyFullAddress");
            if (name != null && address != null) result.add(new Company(name, address,
                    !item.has("active") || item.path("active").asBoolean()));
        }
        return result;
    }
    private static String text(JsonNode n, String... names) {
        for (String name : names) if (n.hasNonNull(name)) return n.path(name).asText();
        return null;
    }
}
