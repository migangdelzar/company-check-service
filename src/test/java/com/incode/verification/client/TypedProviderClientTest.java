package com.incode.verification.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.incode.verification.config.ProviderEndpointProperties;
import com.incode.verification.service.model.NormalizedQuery;
import com.incode.verification.service.model.ProviderResult;
import com.incode.verification.service.model.ProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TypedProviderClientTest {
  @Test
  void mapsTypedHttpPayloadAndPreservesProviderIdentity() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = builder.baseUrl("https://free.test").build();
    var endpoint =
        new ProviderEndpointProperties("https://free.test", "/companies/{query}", "secret");
    var provider = new FreeProviderClient(client, endpoint);

    server
        .expect(requestTo("https://free.test/companies/ACME"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Api-Key", "secret"))
        .andRespond(
            withSuccess(
                "[{\"cin\":\"123\",\"name\":\"Acme\","
                    + "\"registration_date\":\"2020-01-01\",\"address\":\"1 Main St\","
                    + "\"is_active\":true}]",
                MediaType.APPLICATION_JSON));

    var result =
        assertInstanceOf(
            ProviderResult.Success.class, provider.lookup(new NormalizedQuery("ACME")));

    assertEquals(ProviderType.FREE, result.provider());
    assertEquals("Acme", result.companies().getFirst().name());
    server.verify();
  }
}
