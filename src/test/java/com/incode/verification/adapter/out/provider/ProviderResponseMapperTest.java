package com.incode.verification.adapter.out.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProviderResponseMapperTest {
    @Test void mapsPremiumCompanyFullAddress() throws Exception {
        var json = new ObjectMapper().readTree("{\"results\":[{\"companyName\":\"Acme\",\"companyFullAddress\":\"1 Main St\",\"active\":true}]}");
        var result = ProviderResponseMapper.companies(json);
        assertEquals("Acme", result.getFirst().name());
        assertEquals("1 Main St", result.getFirst().address());
    }
}
