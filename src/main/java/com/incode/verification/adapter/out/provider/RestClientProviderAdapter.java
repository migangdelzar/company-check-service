package com.incode.verification.adapter.out.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.incode.verification.application.context.ExecutionContext;
import com.incode.verification.application.port.out.ProviderLookupPort;
import com.incode.verification.domain.type.ProviderFailure;
import com.incode.verification.domain.type.ProviderLookupResult;
import com.incode.verification.domain.type.ProviderType;
import com.incode.verification.domain.valueobject.NormalizedQuery;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public final class RestClientProviderAdapter implements ProviderLookupPort {
  private final RestClient client;
  private final ProviderType type;
  private final ProviderProperties.Endpoint endpoint;
  private final Duration timeout;

  public RestClientProviderAdapter(
      RestClient client,
      ProviderType type,
      ProviderProperties.Endpoint endpoint,
      Duration timeout) {
    this.client = client;
    this.type = type;
    this.endpoint = endpoint;
    this.timeout = timeout;
  }

  @Override
  public ProviderLookupResult lookup(NormalizedQuery query, ExecutionContext context) {
    try {
      JsonNode body =
          client
              .get()
              .uri(endpoint.path(), query.value())
              .header("X-Api-Key", endpoint.apiKey())
              .retrieve()
              .onStatus(
                  HttpStatusCode::is4xxClientError,
                  (request, response) -> {
                    throw new ProviderException(
                        new ProviderFailure.ClientError(response.getStatusCode().value()));
                  })
              .body(JsonNode.class);
      var companies = ProviderResponseMapper.companies(body);
      return companies.isEmpty()
          ? new ProviderLookupResult.Failure(new ProviderFailure.Malformed())
          : new ProviderLookupResult.Success(companies, type);
    } catch (ProviderException e) {
      return new ProviderLookupResult.Failure(e.failure);
    } catch (RestClientResponseException e) {
      return new ProviderLookupResult.Failure(
          new ProviderFailure.ClientError(e.getStatusCode().value()));
    } catch (Exception e) {
      return new ProviderLookupResult.Failure(new ProviderFailure.Unavailable());
    }
  }

  private static final class ProviderException extends RuntimeException {
    final ProviderFailure failure;

    ProviderException(ProviderFailure f) {
      failure = f;
    }
  }
}
